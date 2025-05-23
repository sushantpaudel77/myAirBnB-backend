package com.projects.airbnb.service;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.BookingRequest;
import com.projects.airbnb.dto.GuestDto;
import com.projects.airbnb.entity.*;
import com.projects.airbnb.entity.enums.BookingStatus;
import com.projects.airbnb.repository.*;
import com.projects.airbnb.utility.EntityFinder;
import com.projects.airbnb.utility.HotelField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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

    @Override
    @Transactional
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

        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(
                existingHotel.getId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount());

        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate()) + 1;

/*
        if (inventoryList.size() != daysCount) {
            String message = String.format("Room is not available for the entire stay period. Available for %d out of %d days",
                    inventoryList.size(), daysCount);
            throw new IllegalStateException(message);
        }

//         check if each inventory has enough spaces
        for (Inventory inventory : inventoryList) {
            int availableRooms = inventory.getTotalCount() - inventory.getBookedCount();
            if (availableRooms < bookingRequest.getRoomsCount()) {
                String message = String.format("Not enough rooms available on %s, Available: %d, Requested: %d",
                        inventory.getDate(), availableRooms, bookingRequest.getRoomsCount());
                throw new IllegalArgumentException(message);
            }
        }
*/

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
                .amount(BigDecimal.TEN)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        return modelMapper.map(savedBooking, BookingDto.class);
    }

    @Override
    public BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList) {
        log.info("Adding guests for booking with id: {}", bookingId);
        Booking booking = entityFinder.findByIdOrThrow(bookingRepository, bookingId, HotelField.HOTEL.getKey());

        if (hasBookingHasExpired(booking)) {
            throw new IllegalArgumentException("Booking has already expired");
        }

        for (GuestDto guestDto : guestDtoList) {
            Guest guest = modelMapper.map(guestDto, Guest.class);
            guest.setUser(getCurrentUser());
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        Booking save = bookingRepository.save(booking);
        return modelMapper.map(save, BookingDto.class);

    }

    public boolean hasBookingHasExpired(Booking booking) {
        return booking.getCreatedAt().plusMinutes(10).isBefore(LocalDateTime.now());
    }

    public User getCurrentUser() {
        User user = new User();
        user.setId(1L);
        return user;
    }
}
