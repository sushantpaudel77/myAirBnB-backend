package com.projects.airbnb.controller;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.BookingRequest;
import com.projects.airbnb.dto.GuestDto;
import com.projects.airbnb.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
public class HotelBookingController {

    private final BookingService bookingService;

    @PostMapping(path = "/init")
    public ResponseEntity<BookingDto> initializeBooking(@RequestBody BookingRequest bookingRequest) {
        BookingDto bookingDto = bookingService.initializeBooking(bookingRequest);
        return ResponseEntity.ok(bookingDto);
    }

    @PostMapping(path = "/{bookingId}/addGuests")
    public ResponseEntity<BookingDto> addGuests(@PathVariable Long bookingId,
                                                @RequestBody List<GuestDto> guestDto) {
        BookingDto bookingDto = bookingService.addGuests(bookingId, guestDto);
        return new ResponseEntity<>(bookingDto, HttpStatus.CREATED);
    }

    @PostMapping(path = "/{bookingId}/payments")
    public ResponseEntity<Map<String, String>> initiatePayment(@PathVariable Long bookingId) {
        String sessionUrl = bookingService.initiatePayment(bookingId);
        return ResponseEntity.ok(Map.of("sessionUrl", sessionUrl));
    }

    @PostMapping(path = "/{bookingId}/cancel")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(path = "/{bookingId}/status")
    public ResponseEntity<Map<String, String>> getBookingStatus(@PathVariable Long bookingId) {
        String bookingStatus = bookingService.getBookingStatus(bookingId);
        return ResponseEntity.ok(Map.of("status", bookingStatus));
    }
}
