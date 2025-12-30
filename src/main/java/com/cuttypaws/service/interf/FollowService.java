package com.cuttypaws.service.interf;

import com.cuttypaws.response.FollowResponse;
import org.springframework.data.domain.Pageable;

public interface FollowService {
    FollowResponse followUser(Long targetUserId);
    FollowResponse unfollowUser(Long targetUserId);
    FollowResponse getFollowStats(Long userId);
    FollowResponse getFollowers(Long userId, Pageable pageable);
    FollowResponse getFollowing(Long userId, Pageable pageable);
    FollowResponse checkFollowStatus(Long targetUserId);
    FollowResponse muteUser(Long targetUserId);
    FollowResponse unmuteUser(Long targetUserId);
    FollowResponse getMutualFollowers(Long targetUserId);
}

