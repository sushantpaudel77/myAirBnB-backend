package com.projects.airbnb.service;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.BookingRequest;
import com.projects.airbnb.dto.GuestDto;
import com.projects.airbnb.entity.*;
import com.projects.airbnb.entity.enums.BookingStatus;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.exception.UnAuthorizedException;
import com.projects.airbnb.repository.*;
import com.projects.airbnb.utility.EntityFinder;
import com.projects.airbnb.utility.HotelField;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

        // Validate dates
        if (bookingRequest.getCheckInDate().isAfter(bookingRequest.getCheckOutDate())) {
            throw new IllegalArgumentException("Check-in date cannot be after check-out date");
        }

        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, bookingRequest.getHotelId(), HotelField.HOTEL.getKey());
        Room existingRoom = entityFinder.findByIdOrThrow(roomRepository, bookingRequest.getRoomId(), HotelField.ROOM.getKey());

        long numberOfNights = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());
        if (numberOfNights < 1) {
            throw new IllegalArgumentException("Minimum booking duration is 1 night");
        }

        BigDecimal totalAmount = calculateTotalAmount(existingRoom.getBasePrice(),
                bookingRequest.getRoomsCount(),
                numberOfNights);


        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(
                existingHotel.getId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount());

        // Reserve the room/update the booked count of inventories
        for (Inventory inventory : inventoryList) {
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }

        inventoryRepository.saveAll(inventoryList);

        // create the booking
        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(existingHotel)
                .room(existingRoom)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(getCurrentUser())
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalAmount)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        return modelMapper.map(savedBooking, BookingDto.class);
    }

    private BigDecimal calculateTotalAmount(BigDecimal roomPrice, int roomsCount, long numberOfNights) {
        // Ensure minimum amount meets Stripe's requirement (50 INR = ~0.60 USD)
        BigDecimal baseAmount = roomPrice
                .multiply(BigDecimal.valueOf(roomsCount))
                .multiply(BigDecimal.valueOf(numberOfNights));

        // Minimum amount validation (50 INR)
        return baseAmount.max(BigDecimal.valueOf(50));
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
        if ("checkout.session.completed".equals(event.getType())) {

        } else {
         log.warn("unhandled event type: {}", event.getType());
        }
    }

    public boolean hasBookingHasExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    public User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
