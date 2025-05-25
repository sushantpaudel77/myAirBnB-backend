package com.projects.airbnb.service;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.BookingRequest;
import com.projects.airbnb.dto.GuestDto;
import com.projects.airbnb.dto.HotelReportDto;
import com.projects.airbnb.entity.*;
import com.projects.airbnb.entity.enums.BookingStatus;
import com.projects.airbnb.exception.RefundProcessingException;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.exception.RoomUnavailableException;
import com.projects.airbnb.exception.UnAuthorizedException;
import com.projects.airbnb.repository.*;
import com.projects.airbnb.strategy.PricingService;
import com.projects.airbnb.utility.AppUtils;
import com.projects.airbnb.utility.EntityFinder;
import com.projects.airbnb.utility.HotelField;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.projects.airbnb.utility.AppUtils.getCurrentUser;

@Slf4j
@RequiredArgsConstructor
@Service
public class BookingServiceImpl implements BookingService {

    private final EntityFinder entityFinder;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final GuestRepository guestRepository;
    private final CheckOutService checkOutService;
    private final PricingService pricingService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Transactional
    @Override
    public BookingDto initializeBooking(BookingRequest bookingRequest) {

        log.info("Initializing booking for hotel : {}, room: {}, date {}-{}",
                bookingRequest.getHotelId(),
                bookingRequest.getRoomId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate());

        // Validate dates upfront
        if (bookingRequest.getCheckInDate().isAfter(bookingRequest.getCheckOutDate())) {
            throw new IllegalArgumentException("Check-in date cannot be after check-out date");
        }

        // Validate room count
        if (bookingRequest.getRoomsCount() <= 0) {
            throw new IllegalArgumentException("Rooms count must be greater than zero");
        }

        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, bookingRequest.getHotelId(), HotelField.HOTEL.getKey());
        Room existingRoom = entityFinder.findByIdOrThrow(roomRepository, bookingRequest.getRoomId(), HotelField.ROOM.getKey());

        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(
                existingHotel.getId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount());

        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate()) + 1;

        if (inventoryList.size() != daysCount) {
            log.warn("Not enough inventory for booking request: hotelId={}, roomId={}, requestedRooms={}, availableDays={}",
                    bookingRequest.getHotelId(),
                    bookingRequest.getRoomId(),
                    bookingRequest.getRoomsCount(),
                    inventoryList.size());
            throw new RoomUnavailableException("Room is not available for the entire requested period");
        }

        // reserved the room/ update the booked count of inventories
        inventoryRepository.initBooking(existingRoom.getId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount());

        BigDecimal priceForOneRoom = pricingService.calculateTotalPrice(inventoryList);
        BigDecimal totalPrice = priceForOneRoom.multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

        // create the booking
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(existingHotel)
                .room(existingRoom)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalPrice)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking initialized successfully with ID: {}", savedBooking.getId());

        return modelMapper.map(savedBooking, BookingDto.class);
    }

    @Transactional
    @Override
    public BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList) {
        log.info("Adding guests for booking with id: {}", bookingId);
        Booking booking = entityFinder.findByIdOrThrow(bookingRepository, bookingId, HotelField.HOTEL.getKey());

        User user = getCurrentUser();

        if (!user.equals(booking.getUser())) {
            throw new UnAuthorizedException("Bookings does not belong to this user with ID: " + user.getId());
        }

        if (hasBookingHasExpired(booking)) {
            throw new IllegalArgumentException("Booking has already expired");
        }

        for (GuestDto guestDto : guestDtoList) {
            Guest guest = modelMapper.map(guestDto, Guest.class);
            guest.setUser(user);
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        Booking save = bookingRepository.save(booking);
        return modelMapper.map(save, BookingDto.class);

    }

    @Transactional
    @Override
    public String initiatePayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        User user = getCurrentUser();

        if (!user.equals(booking.getUser())) {
            throw new UnAuthorizedException("Bookings does not belong to this user with ID: " + user.getId());
        }
        if (hasBookingHasExpired(booking)) {
            throw new IllegalArgumentException("Booking has already expired");
        }
        String checkOutSession = checkOutService.getCheckOutSession(booking,
                frontendUrl + "/payments/success",
                frontendUrl + "/payments/failure");

        booking.setBookingStatus(BookingStatus.PAYMENTS_PENDING);
        bookingRepository.save(booking);

        return checkOutSession;
    }

    @Transactional
    @Override
    public void capturePayment(Event event) {
        try {
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);

                if (session == null) {
                    log.error("Stripe session is null for event ID: {}", event.getId());
                    throw new IllegalStateException("Stripe session cannot be null");
                }

                String sessionId = session.getId();
                log.debug("Processing payment capture for session ID: {}", sessionId);

                Booking booking = bookingRepository.findByPaymentSessionId(sessionId)
                        .orElseThrow(() -> {
                            String errorMessage = String.format("Booking not found for session ID: %s", sessionId);
                            log.error(errorMessage);
                            return new ResourceNotFoundException(errorMessage);
                        });

                booking.setBookingStatus(BookingStatus.CONFIRMED);
                bookingRepository.save(booking);

                inventoryRepository.findAndLockAvailableInventory(booking.getRoom().getId(), booking.getCheckInDate(),
                        booking.getCheckOutDate(), booking.getRoomsCount());

                inventoryRepository.confirmBooking(booking.getRoom().getId(), booking.getCheckInDate(),
                        booking.getCheckOutDate(), booking.getRoomsCount());

                log.info("Successfully confirmed booking for Booking ID: {}", booking.getId());
            }
        } catch (ResourceNotFoundException e) {
            log.error("Resource not found while processing payment", e);
        } catch (Exception e) {
            log.error("Unexpected error processing payment for event ID: {}", event.getId(), e);
        }
    }

    @Transactional
    @Override
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    String errorMessage = String.format("Booking not found with ID: %s", bookingId);
                    log.error(errorMessage);
                    return new ResourceNotFoundException(errorMessage);
                });

        User user = AppUtils.getCurrentUser();

        if (!user.equals(booking.getUser())) {
            throw new UnAuthorizedException("Booking does not belong to this user with ID: " + user.getId());
        }

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking ID {} marked as CANCELLED", bookingId);

        inventoryRepository.findAndLockAvailableInventory(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount());
        log.debug("Locked inventory for booking cancellation: Room ID {}, Date range: {} to {}",
                booking.getRoom().getId(), booking.getCheckInDate(), booking.getCheckOutDate());

        inventoryRepository.cancelBooking(
                booking.getRoom().getId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getRoomsCount());
        log.info("Inventory updated after cancellation for Room ID: {}", booking.getRoom().getId());

        // Handle Stripe refund
        try {
            Session session = Session.retrieve(booking.getPaymentSessionId());
            RefundCreateParams refundCreateParams = RefundCreateParams.builder()
                    .setPaymentIntent(session.getPaymentIntent())
                    .build();

            Refund.create(refundCreateParams);
            log.info("Refund initiated for paymentIntent: {}", session.getPaymentIntent());

        } catch (StripeException e) {
            log.error("Stripe exception during refund: {}", e.getMessage(), e);
            throw new RefundProcessingException("Stripe error during refund: " + e.getMessage());
        }
    }

    @Override
    public String getBookingStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() ->
                new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        User user = getCurrentUser();

        if (!user.equals(booking.getUser())) {
            throw new UnAuthorizedException("Bookings does not belong to this user with ID: " + user.getId());
        }
        return booking.getBookingStatus().name();
    }

    @Override
    public List<BookingDto> getAllBookingByHotelId(Long hotelId)  {
        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, hotelId, "Hotel");

        User user = getCurrentUser();

        log.info("getting all bookings for the hotel with ID: {}", hotelId);
        if (!user.equals(existingHotel.getOwner())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not the owner of hotel with ID: " + hotelId);
        }

        List<Booking> bookings = bookingRepository.findByHotel(existingHotel);

        return bookings.stream()
                .map(element -> modelMapper.map(element, BookingDto.class))
                .toList();
    }

    @Override
    public HotelReportDto getHotelReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, hotelId, "Hotel");

        User user = AppUtils.getCurrentUser();

        log.info("Generating report for hotel with ID: {}", hotelId);
        if (!user.equals(existingHotel.getOwner())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not the owner of hotel with ID: " + hotelId);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository.findByHotelAndCreatedAtBetween(existingHotel, startDateTime, endDateTime);

        long totalConfirmedBookings = bookings.stream()
                .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .count();

        BigDecimal totalRevenueOfConfirmedBookings = bookings.stream()
                .filter(booking -> booking.getBookingStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgRevenue = (totalConfirmedBookings == 0)
                ? BigDecimal.ZERO
                : totalRevenueOfConfirmedBookings.divide(BigDecimal.valueOf(totalConfirmedBookings), RoundingMode.HALF_DOWN);

        return new HotelReportDto(totalConfirmedBookings, totalRevenueOfConfirmedBookings, avgRevenue);
    }

    @Override
    public List<BookingDto> getMyBookings() {
        User user = AppUtils.getCurrentUser();


        return bookingRepository.findByUser(user)
                .stream()
                .map(element -> modelMapper.map(element, BookingDto.class))
                .toList();
    }


    public boolean hasBookingHasExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }
}
