package com.projects.airbnb.service;

import com.projects.airbnb.dto.HotelDto;
import com.projects.airbnb.dto.HotelInfoDto;

import java.util.List;
import java.util.Optional;

public interface HotelService {

    HotelDto createNewHotel(HotelDto hotelDto);

    HotelDto getHotelById(Long id);

    HotelDto updateHotelById(Long hotelId, HotelDto updatedHotel);

    void deleteHotelById(Long hotelId);

    void activateHotel(Long hotelId);

    HotelInfoDto getHotelInfoById(Long hotelId);

    List<HotelDto> getAllHotels();
}
