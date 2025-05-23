package com.projects.airbnb.controller;

import com.projects.airbnb.dto.RoomDto;
import com.projects.airbnb.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/admin/hotels/{hotelId}/rooms")
@RequiredArgsConstructor
public class RoomAdminController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomDto> createNewRoom(@PathVariable Long hotelId,
                                                 @RequestBody RoomDto roomDto) {
        RoomDto newRoom = roomService.createNewRoom(hotelId, roomDto);
        return new ResponseEntity<>(newRoom, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getAllRoomsInHotel(@PathVariable Long hotelId) {
        List<RoomDto> allRoomsInHotel = roomService.getAllRoomsInHotel(hotelId);
        return ResponseEntity.ok(allRoomsInHotel);
    }

    @GetMapping(path = "/{roomId}")
    public ResponseEntity<RoomDto> getAllRoomsById(@PathVariable Long hotelId,
                                                   @PathVariable Long roomId) {
        RoomDto allRoomsInHotel = roomService.getRoomById(roomId);
        return ResponseEntity.ok(allRoomsInHotel);
    }

    @DeleteMapping(path = "/{roomId}")
    public ResponseEntity<Void> deleteRoomsById(@PathVariable Long hotelId,
                                                @PathVariable Long roomId) {
        roomService.deleteRoomById(roomId);
        return ResponseEntity.noContent().build();
    }
}
