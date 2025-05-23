package com.projects.airbnb.service;

import com.projects.airbnb.dto.RoomDto;
import com.projects.airbnb.entity.Hotel;
import com.projects.airbnb.entity.Room;
import com.projects.airbnb.repository.HotelRepository;
import com.projects.airbnb.repository.RoomRepository;
import com.projects.airbnb.utility.EntityFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final InventoryService inventoryService;
    private final EntityFinder entityFinder;
    private final ModelMapper modelMapper;
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;


    @Override
    public RoomDto createNewRoom(Long hotelId, RoomDto roomDto) {
        log.info("Create a new hotel with the ID: {}", hotelId);

        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, hotelId, "Hotel");

        Room room = modelMapper.map(roomDto, Room.class);
        room.setHotel(existingHotel);

        Room savedRoom = roomRepository.save(room);

        if (Boolean.TRUE.equals(existingHotel.getIsActive())) {
            inventoryService.initializeRoomForAYear(room);
        }

        return modelMapper.map(savedRoom, RoomDto.class);
    }

    @Override
    public List<RoomDto> getAllRoomsInHotel(Long hotelId) {
        log.info("Getting all rooms in hotel with the ID: {}", hotelId);

        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, hotelId, "Hotel");

        return existingHotel.getRooms()
                .stream()
                .map(element -> modelMapper.map(element, RoomDto.class))
                .toList();
    }

    @Override
    public RoomDto getRoomById(Long roomId) {
        log.info("Getting the room ID: {}", roomId);
        Room existingRoom = entityFinder.findByIdOrThrow(roomRepository, roomId, "Room");
        return modelMapper.map(existingRoom, RoomDto.class);
    }

    @Transactional
    @Override
    public void deleteRoomById(Long roomId) {
        log.info("Deleting the room ID: {}", roomId);
        Room existingRoom = entityFinder.findByIdOrThrow(roomRepository, roomId, "Room");
        inventoryService.deleteAllInventories(existingRoom);
        roomRepository.deleteById(roomId);
    }
}
