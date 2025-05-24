package com.projects.airbnb.controller;

import com.projects.airbnb.dto.LoginDto;
import com.projects.airbnb.dto.LoginResponseDto;
import com.projects.airbnb.dto.SignUpRequestDto;
import com.projects.airbnb.dto.UserDto;
import com.projects.airbnb.security.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping(path = "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(path = "/signUp")
    public ResponseEntity<UserDto> signUp(@RequestBody SignUpRequestDto signUpRequestDto) {
        UserDto userDto = authService.signUp(signUpRequestDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @PostMapping(path = "/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginDto loginDto,
                                                  HttpServletRequest httpServletRequest,
                                                  HttpServletResponse httpServletResponse) {
        String[] tokens = authService.login(loginDto);

        Cookie refreshTokenCookie = new Cookie("refreshToken", tokens[1]);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/"); // makes it available across the application
        // refreshTokenCookie.setSecure(true); // Uncomment in production (for HTTPS)
        // refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // Optional: 7 days

        httpServletResponse.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(new LoginResponseDto(tokens[0]));
    }

    @PostMapping(path = "/refresh")
    public ResponseEntity<LoginResponseDto> refreshToken(HttpServletRequest httpServletRequest) {
         Cookie[] cookies = httpServletRequest.getCookies();

        if (cookies == null) {
            throw new AuthenticationServiceException("No cookies found in the request");
        }

        String refreshToken = Arrays.stream(cookies)
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElseThrow(() -> new AuthenticationServiceException("Refresh token not found in cookies"));

        String newAccessToken = authService.refreshToken(refreshToken);

        return ResponseEntity.ok(new LoginResponseDto(newAccessToken));
    }
}
