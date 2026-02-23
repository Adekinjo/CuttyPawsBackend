package com.cuttypaws.service.interf;


import com.cuttypaws.response.UserResponse;

import java.util.UUID;

public interface UserProfileService {
    UserResponse getUserProfile(UUID userId);
    UserResponse getUserPosts(UUID userId);
    UserResponse blockUser(UUID userId, String reason);
    UserResponse unblockUser(UUID userId);
    UserResponse getUserStats(UUID userId);
}


