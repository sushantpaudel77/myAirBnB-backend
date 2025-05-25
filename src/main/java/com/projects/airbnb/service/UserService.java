package com.projects.airbnb.service;

import com.projects.airbnb.dto.ProfileUpdateRequestDto;
import com.projects.airbnb.entity.User;

public interface UserService {

    User getUserById(Long id);

    void updateProfile(ProfileUpdateRequestDto profileUpdateRequestDto);
}
