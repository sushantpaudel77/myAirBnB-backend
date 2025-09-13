package com.projects.airbnb.service.impl;

import com.projects.airbnb.dto.ProfileUpdateRequestDto;
import com.projects.airbnb.dto.UserDto;
import com.projects.airbnb.entity.User;

public interface UserService {
    User getUserById(Long id);

    void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto);

    UserDto getMyProfile();
}
