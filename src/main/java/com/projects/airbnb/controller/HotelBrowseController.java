package com.projects.airbnb.controller;

import com.projects.airbnb.dto.HotelDto;
import com.projects.airbnb.dto.HotelInfoDto;
import com.projects.airbnb.dto.HotelPriceDto;
import com.projects.airbnb.dto.HotelSearchRequest;
import com.projects.airbnb.service.HotelService;
import com.projects.airbnb.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/hotels")
@RequiredArgsConstructor
public class HotelBrowseController {

    private final InventoryService inventoryService;
    private final HotelService hotelService;

    @GetMapping(path = "/search")
    public ResponseEntity<Page<HotelPriceDto>> searchHotels(
            @RequestBody HotelSearchRequest hotelSearchRequest) {
        Page<HotelPriceDto> page = inventoryService.searchHotels(hotelSearchRequest);
        return ResponseEntity.ok(page);
    }

    @GetMapping(path = "/{hotelId}/info")
    public ResponseEntity<HotelInfoDto> getHotelInfo(@PathVariable Long hotelId) {
        HotelInfoDto hotelInfoById = hotelService.getHotelInfoById(hotelId);
        return ResponseEntity.ok(hotelInfoById);
    }
}
