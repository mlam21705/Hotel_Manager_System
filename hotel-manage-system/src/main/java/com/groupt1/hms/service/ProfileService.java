package com.groupt1.hms.service;

import com.groupt1.hms.dto.UserProfileDTO;

public interface ProfileService {
    
    UserProfileDTO getUserProfile(Integer userId);

    void updateUserProfile(Integer userId, UserProfileDTO dto);
}
