package com.cuttypaws.service.interf;

import com.cuttypaws.response.FollowResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FollowService {
    FollowResponse followUser(UUID targetUserId);
    FollowResponse unfollowUser(UUID targetUserId);
    FollowResponse getFollowStats(UUID userId);
    FollowResponse getFollowers(UUID userId, Pageable pageable);
    FollowResponse getFollowing(UUID userId, Pageable pageable);
    FollowResponse checkFollowStatus(UUID targetUserId);
    FollowResponse muteUser(UUID targetUserId);
    FollowResponse unmuteUser(UUID targetUserId);
    FollowResponse getMutualFollowers(UUID targetUserId);
}

