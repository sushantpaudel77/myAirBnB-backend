package com.projects.airbnb.security;

import com.projects.airbnb.dto.LoginDto;
import com.projects.airbnb.dto.SignUpRequestDto;
import com.projects.airbnb.dto.UserDto;
import com.projects.airbnb.entity.User;
import com.projects.airbnb.entity.enums.Role;
import com.projects.airbnb.exception.ResourceAlreadyExistsException;
import com.projects.airbnb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public UserDto signUp(SignUpRequestDto signUpRequestDto) {

        User user = userRepository.findByEmail(signUpRequestDto.getEmail()).orElse(null);

        if (user != null) {
            throw new ResourceAlreadyExistsException("User is already present with the same email");
        }

        User newUser = modelMapper.map(signUpRequestDto, User.class);
        newUser.setRoles(Set.of(Role.GUEST));
        newUser.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        newUser = userRepository.save(newUser);

        return modelMapper.map(newUser, UserDto.class);

    }

    public String[] login(LoginDto loginDto) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getEmail(),
                loginDto.getPassword()
        ));

        User user = (User) authenticate.getPrincipal();

        String[] arr = new String[2];
        arr[0] = jwtUtil.buildToken(user);
        arr[1] = jwtUtil.generateRefreshToken(user);

        return arr;
    }
}
