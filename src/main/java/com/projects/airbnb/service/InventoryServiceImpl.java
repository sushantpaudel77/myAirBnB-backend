package com.projects.airbnb.service;

import com.projects.airbnb.dto.HotelPriceDto;
import com.projects.airbnb.dto.HotelSearchRequest;
import com.projects.airbnb.dto.InventoryDto;
import com.projects.airbnb.dto.UpdateInventoryRequestDto;
import com.projects.airbnb.entity.Inventory;
import com.projects.airbnb.entity.Room;
import com.projects.airbnb.entity.User;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.repository.HotelMinPriceRepository;
import com.projects.airbnb.repository.InventoryRepository;
import com.projects.airbnb.repository.RoomRepository;
import com.projects.airbnb.utility.AppUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.projects.airbnb.utility.AppUtils.getCurrentUser;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;
    private final HotelMinPriceRepository hotelMinPriceRepository;
    private final RoomRepository roomRepository;

    @Override
    public void initializeRoomForAYear(Room room) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);

        for (; !today.isAfter(endDate); today = today.plusDays(1)) {
            boolean exists = inventoryRepository.existsByRoomAndDate(room, today);
            if (exists) continue;

            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoryRepository.save(inventory);
        }
    }

    @Override
    public void deleteAllInventories(Room room) {
        log.info("Deleting the inventories of room with ID: {}", room.getId());
        inventoryRepository.deleteByRoom(room);
    }

    @Override
    public Page<HotelPriceDto> searchHotels(HotelSearchRequest hotelSearchRequest) {
        log.info("Searching hotels for {} city, from {} to {}", hotelSearchRequest.getCity(), hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate());
        Pageable pageable = PageRequest.of(hotelSearchRequest.getPage(), hotelSearchRequest.getSize());
        long dateCount = ChronoUnit.DAYS.between(hotelSearchRequest.getStartDate(), hotelSearchRequest.getEndDate()) + 1;


        return hotelMinPriceRepository.findHotelWithAvailableInventory(
                hotelSearchRequest.getCity(),
                hotelSearchRequest.getStartDate(),
                hotelSearchRequest.getEndDate(),
                hotelSearchRequest.getRoomsCount(),
                dateCount,
                pageable);
    }

    @Override
    public List<InventoryDto> getAllInventoryByRoom(Long roomId) {
        log.info("Getting All inventory by room for room with ID: {}", roomId);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        User currentUser = AppUtils.getCurrentUser();
        if (!currentUser.equals(room.getHotel().getOwner())) {
            throw new AccessDeniedException("You are not the owner of room with ID: " + roomId);
        }

        return inventoryRepository.findByRoomOrderByRoom(room)
                .stream()
                .map(element -> modelMapper.map(element, InventoryDto.class))
                .toList();
    }

    @Transactional
    @Override
    public void updateInventory(Long roomId, UpdateInventoryRequestDto updateInventoryRequestDto) {
        log.info("[START] Updating inventory for Room ID: {} | Date Range: {} to {}",
                roomId, updateInventoryRequestDto.getStartDate(), updateInventoryRequestDto.getEndDate());

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.warn("[ROOM NOT FOUND] Room ID: {}", roomId);
                    return new ResourceNotFoundException("Room not found with ID: " + roomId);
                });

        User currentUser = AppUtils.getCurrentUser();
        log.debug("Current currentUser ID: {} attempting to update Room ID: {} owned by User ID: {}",
                currentUser.getId(), roomId, room.getHotel().getOwner().getId());

        if (!currentUser.equals(room.getHotel().getOwner())) {
            log.error("[ACCESS DENIED] User ID: {} is not the owner of Room ID: {}", currentUser.getId(), roomId);
            throw new AccessDeniedException("You are not the owner of room with ID: " + roomId);
        }

        log.info("[LOCKING] Attempting to lock inventory records for update...");
        inventoryRepository.getInventoryAndLockBeforeUpdate(
                roomId,
                updateInventoryRequestDto.getStartDate(),
                updateInventoryRequestDto.getEndDate()
        );
        log.info("[LOCK ACQUIRED] Locked inventory records for Room ID: {} from {} to {}",
                roomId, updateInventoryRequestDto.getStartDate(), updateInventoryRequestDto.getEndDate());

        log.info("[UPDATING INVENTORY] Setting closed: {}, surgeFactor: {}",
                updateInventoryRequestDto.getClosed(), updateInventoryRequestDto.getSurgeFactor());

        inventoryRepository.updateInventory(
                roomId,
                updateInventoryRequestDto.getStartDate(),
                updateInventoryRequestDto.getEndDate(),
                updateInventoryRequestDto.getClosed(),
                updateInventoryRequestDto.getSurgeFactor()
        );

        log.info("[SUCCESS] Inventory updated successfully for Room ID: {}", roomId);
    }

}
