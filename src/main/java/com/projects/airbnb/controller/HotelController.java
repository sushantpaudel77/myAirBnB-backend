package com.projects.airbnb.controller;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.HotelDto;
import com.projects.airbnb.dto.HotelReportDto;
import com.projects.airbnb.service.impl.BookingService;
import com.projects.airbnb.service.impl.HotelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/admin/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<HotelDto> createNewHotel(@RequestBody HotelDto hotelDto) {
        log.info("attempting to create a new hotel with name: {}", hotelDto.getName());
        HotelDto hotel = hotelService.createNewHotel(hotelDto);
        return new ResponseEntity<>(hotel, HttpStatus.CREATED);
    }

    @GetMapping(path = "/{hotelId}")
    public ResponseEntity<HotelDto> getHotelById(@PathVariable Long hotelId) {
        HotelDto hotelDto = hotelService.getHotelById(hotelId);
        return ResponseEntity.ok(hotelDto);
    }

    @PutMapping(path = "/{hotelId}")
    public ResponseEntity<HotelDto> updateHotelById(@PathVariable Long hotelId,
                                                    @RequestBody HotelDto updatedHotel) {
        HotelDto hotelDto = hotelService.updateHotelById(hotelId, updatedHotel);
        return ResponseEntity.ok(hotelDto);
    }

    @DeleteMapping(path = "/{hotelId}")
    public ResponseEntity<Void> deleteHotelById(@PathVariable Long hotelId) {
        hotelService.deleteHotelById(hotelId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(path = "/{hotelId}/activate")
    public ResponseEntity<HotelDto> activateHotel(@PathVariable Long hotelId) {
        hotelService.activateHotel(hotelId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<HotelDto>> getAllHotels() {
        List<HotelDto> allHotels = hotelService.getAllHotels();
        return ResponseEntity.ok(allHotels);
    }

    @GetMapping("/{hotelId}/bookings")
    public ResponseEntity<List<BookingDto>> getAllBookingByHotelId(@PathVariable Long hotelId)  {
        List<BookingDto> bookingDto = bookingService.getAllBookingByHotelId(hotelId);
        return ResponseEntity.ok(bookingDto);
    }

    @GetMapping("/{hotelId}/reports")
    public ResponseEntity<HotelReportDto> getHotelReport(@PathVariable Long hotelId,
                                                               @RequestParam(required = false) LocalDate startDate,
                                                               @RequestParam(required = false)LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        HotelReportDto hotelReport = bookingService.getHotelReport(hotelId, startDate, endDate);
        return ResponseEntity.ok(hotelReport);
    }

}
