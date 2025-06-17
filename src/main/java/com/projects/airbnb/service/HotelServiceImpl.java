package com.projects.airbnb.service;

import com.projects.airbnb.dto.HotelDto;
import com.projects.airbnb.dto.HotelInfoDto;
import com.projects.airbnb.dto.RoomDto;
import com.projects.airbnb.entity.Hotel;
import com.projects.airbnb.entity.Room;
import com.projects.airbnb.entity.User;
import com.projects.airbnb.exception.ResourceNotFoundException;
import com.projects.airbnb.exception.UnAuthorizedException;
import com.projects.airbnb.repository.HotelRepository;
import com.projects.airbnb.repository.RoomRepository;
import com.projects.airbnb.utility.AppUtils;
import com.projects.airbnb.utility.EntityFinder;
import com.projects.airbnb.utility.HotelField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.projects.airbnb.utility.AppUtils.getCurrentUser;


@Slf4j
@Service
@RequiredArgsConstructor
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final ModelMapper modelMapper;
    private final EntityFinder entityFinder;
    private final InventoryService inventoryService;
    private final RoomRepository roomRepository;

    @Override
    public HotelDto createNewHotel(HotelDto hotelDto) {
        log.info("Creating a new hotel with name: {}", hotelDto.getName());

        Hotel hotel = modelMapper.map(hotelDto, Hotel.class);
        hotel.setIsActive(false);

        User currentUser = AppUtils.getCurrentUser();
        hotel.setOwner(currentUser);

        hotel = hotelRepository.save(hotel);

        log.info("Created a new hotel with ID: {}", hotel.getId());
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto getHotelById(Long id) {
        log.info("Getting the hotel with ID: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException
                        ("Hotel not found with ID: " + id));
        User currentUser = AppUtils.getCurrentUser();

        if (!currentUser.equals(hotel.getOwner())) {
            throw new UnAuthorizedException("This currentUser does not own this hotel with ID: " + id);
        }
        return modelMapper.map(hotel, HotelDto.class);
    }

    @Override
    public HotelDto updateHotelById(Long hotelId, HotelDto updatedHotel) {
        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, hotelId, HotelField.HOTEL.getKey());

        User user = AppUtils.getCurrentUser();

        if (!user.equals(existingHotel.getOwner())) {
            throw new UnAuthorizedException("This user does not own this hotel with ID: " + hotelId);
        }

        // Map new values over the existing object
        modelMapper.map(updatedHotel, existingHotel);
        existingHotel.setId(hotelId);

        Hotel savedHotel = hotelRepository.save(existingHotel);
        return modelMapper.map(savedHotel, HotelDto.class);
    }


    @Transactional
    @Override
    public void deleteHotelById(Long hotelId) {
        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, hotelId, HotelField.HOTEL.getKey());

        User user = AppUtils.getCurrentUser();

        if (!user.equals(existingHotel.getOwner())) {
            throw new UnAuthorizedException("This user does not own this hotel with ID: " + hotelId);
        }

        for (Room room : existingHotel.getRooms()) {
            inventoryService.deleteAllInventories(room);
            roomRepository.deleteById(room.getId());
        }
        hotelRepository.deleteById(hotelId);
    }


    @Transactional
    @Override
    public void activateHotel(Long hotelId) {
        log.info("Activating the hotel with ID: {}", hotelId);
        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, hotelId, HotelField.HOTEL.getKey());

        User user = AppUtils.getCurrentUser();

        if (!user.equals(existingHotel.getOwner())) {
            throw new UnAuthorizedException("This user does not own this hotel with ID: " + hotelId);
        }

        existingHotel.setIsActive(true);

        // assuming only do it once
        for (Room room : existingHotel.getRooms()) {
            inventoryService.initializeRoomForAYear(room);
        }
    }

    // public
    @Override
    public HotelInfoDto getHotelInfoById(Long hotelId) {
        Hotel existingHotel = entityFinder.findByIdOrThrow(hotelRepository, hotelId, HotelField.HOTEL.getKey());

        List<RoomDto> rooms = existingHotel.getRooms()
                .stream()
                .map(element -> modelMapper.map(element, RoomDto.class))
                .toList();

        return new HotelInfoDto(modelMapper.map(existingHotel, HotelDto.class), rooms);
    }

    @Override
    public List<HotelDto> getAllHotels() {
        User user = getCurrentUser();
        log.info("getting all hotels for the admin user with ID: {}", user.getId());
        List<Hotel> hotels = hotelRepository.findByOwner(user);
        return hotels
                .stream()
                .map(element -> modelMapper.map(element, HotelDto.class))
                .toList();
    }
}
