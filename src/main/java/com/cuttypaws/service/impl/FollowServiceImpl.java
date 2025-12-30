package com.cuttypaws.service.impl;

import com.cuttypaws.dto.FollowDto;
import com.cuttypaws.dto.FollowStatsDto;
import com.cuttypaws.dto.UserBasicDto;
import com.cuttypaws.entity.Follow;
import com.cuttypaws.entity.User;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.FollowResponse;
import com.cuttypaws.service.interf.FollowService;
import com.cuttypaws.service.interf.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl implements FollowService {

    private final FollowRepo followRepository;
    private final UserRepo userRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public FollowResponse followUser(Long targetUserId) {
        try {
            User currentUser = getCurrentUser();

            if (currentUser.getId().equals(targetUserId)) {
                return FollowResponse.builder()
                        .status(400)
                        .message("You cannot follow yourself")
                        .timeStamp(LocalDateTime.now())
                        .build();
            }

            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + targetUserId));

            if (targetUser.getIsBlocked() != null && targetUser.getIsBlocked()) {
                return FollowResponse.builder()
                        .status(400)
                        .message("Cannot follow a blocked user")
                        .timeStamp(LocalDateTime.now())
                        .build();
            }

            if (followRepository.existsByFollowerAndFollowing(currentUser, targetUser)) {
                return FollowResponse.builder()
                        .status(400)
                        .message("You are already following this user")
                        .timeStamp(LocalDateTime.now())
                        .build();
            }

            Follow follow = Follow.builder()
                    .follower(currentUser)
                    .following(targetUser)
                    .isMuted(false)
                    .build();

            followRepository.save(follow);

            notificationService.sendFollowNotification(
                    targetUser.getId(),      // recipient
                    currentUser.getId()      // sender
            );

            log.info("User {} started following user {}", currentUser.getEmail(), targetUser.getEmail());

            return FollowResponse.builder()
                    .status(200)
                    .message("Successfully followed user")
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            log.error("Follow failed: {}", e.getMessage());
            return FollowResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error following user: {}", e.getMessage(), e);
            return FollowResponse.builder()
                    .status(500)
                    .message("Failed to follow user")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public FollowResponse unfollowUser(Long targetUserId) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + targetUserId));

            if (!followRepository.existsByFollowerAndFollowing(currentUser, targetUser)) {
                return FollowResponse.builder()
                        .status(400)
                        .message("You are not following this user")
                        .timeStamp(LocalDateTime.now())
                        .build();
            }

            followRepository.deleteByFollowerIdAndFollowingId(currentUser.getId(), targetUserId);

            log.info("User {} unfollowed user {}", currentUser.getEmail(), targetUser.getEmail());

            return FollowResponse.builder()
                    .status(200)
                    .message("Successfully unfollowed user")
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            log.error("Unfollow failed: {}", e.getMessage());
            return FollowResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error unfollowing user: {}", e.getMessage(), e);
            return FollowResponse.builder()
                    .status(500)
                    .message("Failed to unfollow user")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FollowResponse getFollowStats(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

            Long followersCount = followRepository.countByFollowingId(userId);
            Long followingCount = followRepository.countByFollowerId(userId);

            Boolean isFollowing = false;
            Boolean isFollowedBy = false;

            try {
                User currentUser = getCurrentUser();
                isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), userId);
                isFollowedBy = followRepository.existsByFollowerIdAndFollowingId(userId, currentUser.getId());
            } catch (Exception e) {
                log.debug("No authenticated user for follow status check");
            }

            FollowStatsDto stats = FollowStatsDto.builder()
                    .userId(userId)
                    .followersCount(followersCount)
                    .followingCount(followingCount)
                    .isFollowing(isFollowing)
                    .isFollowedBy(isFollowedBy)
                    .build();

            return FollowResponse.builder()
                    .status(200)
                    .message("Follow stats retrieved successfully")
                    .followStats(stats)
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            log.error("Get follow stats failed: {}", e.getMessage());
            return FollowResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error getting follow stats: {}", e.getMessage(), e);
            return FollowResponse.builder()
                    .status(500)
                    .message("Failed to get follow stats")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FollowResponse getFollowers(Long userId, Pageable pageable) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

            Page<Follow> followersPage = followRepository.findFollowersByUserId(userId, pageable);

            List<FollowDto> followerDtos = followersPage.getContent().stream()
                    .map(this::mapFollowToDto)
                    .collect(Collectors.toList());

            return FollowResponse.builder()
                    .status(200)
                    .message("Followers retrieved successfully")
                    .followersList(followerDtos)
                    .totalPage(followersPage.getTotalPages())
                    .totalElement(followersPage.getTotalElements())
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            log.error("Get followers failed: {}", e.getMessage());
            return FollowResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error getting followers: {}", e.getMessage(), e);
            return FollowResponse.builder()
                    .status(500)
                    .message("Failed to get followers")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FollowResponse getFollowing(Long userId, Pageable pageable) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

            Page<Follow> followingPage = followRepository.findFollowingByUserId(userId, pageable);

            List<FollowDto> followingDtos = followingPage.getContent().stream()
                    .map(this::mapFollowToDto)
                    .collect(Collectors.toList());

            return FollowResponse.builder()
                    .status(200)
                    .message("Following retrieved successfully")
                    .followingList(followingDtos)
                    .totalPage(followingPage.getTotalPages())
                    .totalElement(followingPage.getTotalElements())
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            log.error("Get following failed: {}", e.getMessage());
            return FollowResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error getting following: {}", e.getMessage(), e);
            return FollowResponse.builder()
                    .status(500)
                    .message("Failed to get following")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FollowResponse checkFollowStatus(Long targetUserId) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + targetUserId));

            Boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), targetUserId);
            Boolean isFollowedBy = followRepository.existsByFollowerIdAndFollowingId(targetUserId, currentUser.getId());

            FollowStatsDto status = FollowStatsDto.builder()
                    .userId(targetUserId)
                    .isFollowing(isFollowing)
                    .isFollowedBy(isFollowedBy)
                    .build();

            return FollowResponse.builder()
                    .status(200)
                    .message("Follow status retrieved successfully")
                    .followStats(status)
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            log.error("Check follow status failed: {}", e.getMessage());
            return FollowResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error checking follow status: {}", e.getMessage(), e);
            return FollowResponse.builder()
                    .status(500)
                    .message("Failed to check follow status")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public FollowResponse muteUser(Long targetUserId) {
        try {
            User currentUser = getCurrentUser();
            Follow follow = followRepository.findByFollowerAndFollowing(currentUser,
                            userRepository.findById(targetUserId)
                                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + targetUserId)))
                    .orElseThrow(() -> new NotFoundException("You are not following this user"));

            follow.setIsMuted(true);
            followRepository.save(follow);

            log.info("User {} muted user {}", currentUser.getEmail(), targetUserId);

            return FollowResponse.builder()
                    .status(200)
                    .message("User muted successfully")
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            log.error("Mute user failed: {}", e.getMessage());
            return FollowResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error muting user: {}", e.getMessage(), e);
            return FollowResponse.builder()
                    .status(500)
                    .message("Failed to mute user")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional
    public FollowResponse unmuteUser(Long targetUserId) {
        try {
            User currentUser = getCurrentUser();
            Follow follow = followRepository.findByFollowerAndFollowing(currentUser,
                            userRepository.findById(targetUserId)
                                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + targetUserId)))
                    .orElseThrow(() -> new NotFoundException("You are not following this user"));

            follow.setIsMuted(false);
            followRepository.save(follow);

            log.info("User {} unmuted user {}", currentUser.getEmail(), targetUserId);

            return FollowResponse.builder()
                    .status(200)
                    .message("User unmuted successfully")
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            log.error("Unmute user failed: {}", e.getMessage());
            return FollowResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error unmuting user: {}", e.getMessage(), e);
            return FollowResponse.builder()
                    .status(500)
                    .message("Failed to unmute user")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FollowResponse getMutualFollowers(Long targetUserId) {
        try {
            User currentUser = getCurrentUser();
            User targetUser = userRepository.findById(targetUserId)
                    .orElseThrow(() -> new NotFoundException("User not found with ID: " + targetUserId));

            // Get users that both current user and target user follow
            List<Follow> currentUserFollowing = followRepository.findAllFollowingByUserId(currentUser.getId());
            List<Follow> targetUserFollowing = followRepository.findAllFollowingByUserId(targetUserId);

            List<Long> currentFollowingIds = currentUserFollowing.stream()
                    .map(follow -> follow.getFollowing().getId())
                    .collect(Collectors.toList());

            List<FollowDto> mutualFollows = targetUserFollowing.stream()
                    .filter(follow -> currentFollowingIds.contains(follow.getFollowing().getId()))
                    .map(this::mapFollowToDto)
                    .collect(Collectors.toList());

            return FollowResponse.builder()
                    .status(200)
                    .message("Mutual followers retrieved successfully")
                    .followingList(mutualFollows)
                    .timeStamp(LocalDateTime.now())
                    .build();

        } catch (NotFoundException e) {
            log.error("Get mutual followers failed: {}", e.getMessage());
            return FollowResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .timeStamp(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Error getting mutual followers: {}", e.getMessage(), e);
            return FollowResponse.builder()
                    .status(500)
                    .message("Failed to get mutual followers")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    private FollowDto mapFollowToDto(Follow follow) {
        UserBasicDto followerDto = UserBasicDto.builder()
                .id(follow.getFollower().getId())
                .name(follow.getFollower().getName())
                .email(follow.getFollower().getEmail())
                .profileImageUrl(follow.getFollower().getProfileImageUrl())
                .isBlocked(follow.getFollower().getIsBlocked())
                .isActive(follow.getFollower().isActive())
                .build();

        UserBasicDto followingDto = UserBasicDto.builder()
                .id(follow.getFollowing().getId())
                .name(follow.getFollowing().getName())
                .email(follow.getFollowing().getEmail())
                .profileImageUrl(follow.getFollowing().getProfileImageUrl())
                .isBlocked(follow.getFollowing().getIsBlocked())
                .isActive(follow.getFollowing().isActive())
                .build();

        return FollowDto.builder()
                .id(follow.getId())
                .follower(followerDto)
                .following(followingDto)
                .createdAt(follow.getCreatedAt())
                .isMuted(follow.getIsMuted())
                .build();
    }
}