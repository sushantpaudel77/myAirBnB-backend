package com.projects.airbnb.controller;

import com.projects.airbnb.dto.BookingDto;
import com.projects.airbnb.dto.ProfileUpdateRequestDto;
import com.projects.airbnb.dto.UserDto;
import com.projects.airbnb.service.impl.BookingService;
import com.projects.airbnb.service.impl.UserService;
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

    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody ProfileUpdateRequestDto profileUpdateRequestDto) {
        userService.updateProfile(profileUpdateRequestDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/myBookings")
    public ResponseEntity<List<BookingDto>> getBookingsForCurrentUser() {
        List<BookingDto> userBookings = bookingService.getMyBookings();
        return ResponseEntity.ok(userBookings);
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        UserDto userProfile = userService.getMyProfile();
        return ResponseEntity.ok(userProfile);
    }
}
