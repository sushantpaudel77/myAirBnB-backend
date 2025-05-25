package com.projects.airbnb.service;

import com.projects.airbnb.dto.HotelDto;
import com.projects.airbnb.dto.HotelPriceDto;
import com.projects.airbnb.dto.HotelSearchRequest;
import com.projects.airbnb.dto.InventoryDto;
import com.projects.airbnb.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest);

    List<InventoryDto> getAllInventoryByRoom(Long roomId);
}
