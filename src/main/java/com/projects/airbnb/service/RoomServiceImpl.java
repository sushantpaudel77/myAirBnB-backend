package com.projects.airbnb.service;

import com.projects.airbnb.dto.RoomDto;
import com.projects.airbnb.entity.Hotel;
import com.projects.airbnb.entity.Room;
import com.projects.airbnb.entity.User;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.exception.UnAuthorizedException;
import com.projects.airbnb.repository.HotelRepository;
import com.projects.airbnb.repository.RoomRepository;
import com.projects.airbnb.utility.EntityFinder;
import com.projects.airbnb.utility.HotelField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.projects.airbnb.utility.AppUtils.getCurrentUser;

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

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(existingHotel.getOwner())) {
            throw new UnAuthorizedException("This user does not own this hotel with ID: " + hotelId);
        }

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

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(existingHotel.getOwner())) {
            throw new UnAuthorizedException("This user does not own this hotel with ID: " + hotelId);
        }

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

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!user.equals(existingRoom.getHotel().getOwner())) {
            throw new UnAuthorizedException("This user does not own this room with ID: " + roomId);
        }

        inventoryService.deleteAllInventories(existingRoom);
        roomRepository.deleteById(roomId);
    }

    @Transactional
    @Override
    public RoomDto updateRoomById(Long hotelId, Long roomId, RoomDto roomDto) {
        log.info("Updating the room with ID: {}", roomId);
        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, hotelId, HotelField.HOTEL.getKey());

        User user = getCurrentUser();

        if (!user.equals(existingHotel.getOwner())) {
            throw new UnAuthorizedException("This user does not own this hotel with ID: " + hotelId);
        }
        Room room = roomRepository.findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomDto));

        modelMapper.map(roomDto, room);
        room.setId(roomId);

        room = roomRepository.save(room);

        return modelMapper.map(room, RoomDto.class);
    }
}
