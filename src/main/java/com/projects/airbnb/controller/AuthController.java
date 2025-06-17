package com.projects.airbnb.controller;

import com.projects.airbnb.dto.LoginDto;
import com.projects.airbnb.dto.LoginResponseDto;
import com.projects.airbnb.dto.SignUpRequestDto;
import com.projects.airbnb.dto.UserDto;
import com.projects.airbnb.security.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account using the provided sign-up information."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid sign-up data")
    })
    @PostMapping(path = "/signUp")
    public ResponseEntity<UserDto> signUp(@RequestBody SignUpRequestDto signUpRequestDto) {
        UserDto userDto = authService.signUp(signUpRequestDto);
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Authenticate user and login",
            description = "Validates user credentials and returns an access token. Sets a secure HTTP-only refresh token cookie."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping(path = "/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginDto loginDto,
                                                  HttpServletRequest httpServletRequest,
                                                  HttpServletResponse httpServletResponse) {
        String[] tokens = authService.login(loginDto);

        Cookie refreshTokenCookie = new Cookie("refreshToken", tokens[1]);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/"); // Cookie available for entire app
        // refreshTokenCookie.setSecure(true); // Enable in production (HTTPS only)
        // refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // Optional: Set expiry (7 days)

        httpServletResponse.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(new LoginResponseDto(tokens[0]));
    }

    @Operation(
            summary = "Refresh access token",
            description = "Uses the refresh token stored in HTTP-only cookie to generate a new access token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New access token generated"),
            @ApiResponse(responseCode = "401", description = "Refresh token missing or invalid")
    })
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
