package com.cuttypaws.service.interf;


import com.cuttypaws.response.UserResponse;

public interface UserProfileService {
    UserResponse getUserProfile(Long userId);
    UserResponse getUserPosts(Long userId);
    UserResponse blockUser(Long userId, String reason);
    UserResponse unblockUser(Long userId);
    UserResponse getUserStats(Long userId);
}


