package com.projects.airbnb.controller;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.ProfileUpdateRequestDto;
import com.projects.airbnb.service.BookingService;
import com.projects.airbnb.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final BookingService bookingService;

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody ProfileUpdateRequestDto profileUpdateRequestDto) {
        userService.updateProfile(profileUpdateRequestDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/myBookings")
    public ResponseEntity<List<BookingDto>> getMyBookings() {

        return ResponseEntity.ok(bookingService.getMyBookings());
    }
}
