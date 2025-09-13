package com.projects.airbnb.service.impl;

import com.projects.airbnb.dto.HotelPriceDto;
import com.projects.airbnb.dto.HotelSearchRequest;
import com.projects.airbnb.dto.InventoryDto;
import com.projects.airbnb.dto.UpdateInventoryRequestDto;
import com.projects.airbnb.entity.Room;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InventoryService {

    void initializeRoomForAYear(Room room);

    void deleteAllInventories(Room room);

    Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest);

    List<InventoryDto> getAllInventoryByRoom(Long roomId);

    void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto);
}
