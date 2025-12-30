package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.exception.*;
import com.cuttypaws.mapper.PostMapper;
import com.cuttypaws.mapper.UserMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.*;
import com.cuttypaws.service.interf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepo userRepo;
    private final PostRepo postRepo;
    private final PostLikeRepo postLikeRepo;
    private final UserMapper mapper;
    private final PostMapper postMapper;
    private final FollowRepo followRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        try {
            log.info("Fetching profile for user ID: {}", userId);

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // Check if user is blocked
            if (Boolean.TRUE.equals(user.getIsBlocked())) {
                return UserResponse.builder()
                        .status(403)
                        .message("This user profile has been blocked")
                        .build();
            }

            UserDto userDto = mapper.mapUserToDtoBasic(user);

            log.info("Successfully fetched profile for user: {}", user.getEmail());
            return UserResponse.builder()
                    .status(200)
                    .message("User profile retrieved successfully")
                    .user(userDto)
                    .build();

        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", userId);
            return UserResponse.builder()
                    .status(404)
                    .message("User not found")
                    .build();
        } catch (Exception e) {
            log.error("Error fetching user profile for ID {}: {}", userId, e.getMessage(), e);
            return UserResponse.builder()
                    .status(500)
                    .message("Failed to fetch user profile")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserPosts(Long userId) {
        try {
            log.info("Fetching posts for user ID: {}", userId);

            // Verify user exists and is not blocked
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            if (Boolean.TRUE.equals(user.getIsBlocked())) {
                return UserResponse.builder()
                        .status(403)
                        .message("This user profile has been blocked")
                        .build();
            }

            List<Post> userPosts = postRepo.findByOwnerIdOrderByCreatedAtDesc(userId);

            // Get current user ID for like status
            Long currentUserId = getCurrentUserId();

            List<PostDto> postDtos = userPosts.stream()
                    .map(post -> postMapper.mapPostToDto(post, currentUserId))
                    .collect(Collectors.toList());

            log.info("Successfully fetched {} posts for user: {}", postDtos.size(), user.getEmail());
            return UserResponse.builder()
                    .status(200)
                    .message("User posts retrieved successfully")
                    .postList(postDtos)
                    .build();

        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", userId);
            return UserResponse.builder()
                    .status(404)
                    .message("User not found")
                    .build();
        } catch (Exception e) {
            log.error("Error fetching user posts for ID {}: {}", userId, e.getMessage(), e);
            return UserResponse.builder()
                    .status(500)
                    .message("Failed to fetch user posts")
                    .build();
        }
    }

    @Override
    @Transactional
    public UserResponse blockUser(Long userId, String reason) {
        try {
            log.info("Blocking user ID: {} with reason: {}", userId, reason);

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            user.setIsBlocked(true);
            user.setBlockedReason(reason);
            user.setBlockedAt(LocalDateTime.now());

            User savedUser = userRepo.save(user);

            log.info("Successfully blocked user: {}", user.getEmail());
            return UserResponse.builder()
                    .status(200)
                    .message("User blocked successfully")
                    .user(mapper.mapUserToDtoBasic(savedUser))
                    .build();

        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", userId);
            return UserResponse.builder()
                    .status(404)
                    .message("User not found")
                    .build();
        } catch (Exception e) {
            log.error("Error blocking user ID {}: {}", userId, e.getMessage(), e);
            return UserResponse.builder()
                    .status(500)
                    .message("Failed to block user")
                    .build();
        }
    }

    @Override
    @Transactional
    public UserResponse unblockUser(Long userId) {
        try {
            log.info("Unblocking user ID: {}", userId);

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            user.setIsBlocked(false);
            user.setBlockedReason(null);
            user.setBlockedAt(null);

            User savedUser = userRepo.save(user);

            log.info("Successfully unblocked user: {}", user.getEmail());
            return UserResponse.builder()
                    .status(200)
                    .message("User unblocked successfully")
                    .user(mapper.mapUserToDtoBasic(savedUser))
                    .build();

        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", userId);
            return UserResponse.builder()
                    .status(404)
                    .message("User not found")
                    .build();
        } catch (Exception e) {
            log.error("Error unblocking user ID {}: {}", userId, e.getMessage(), e);
            return UserResponse.builder()
                    .status(500)
                    .message("Failed to unblock user")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserStats(Long userId) {
        try {
            log.info("Fetching stats for user ID: {}", userId);

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            if (Boolean.TRUE.equals(user.getIsBlocked())) {
                return UserResponse.builder()
                        .status(403)
                        .message("This user profile has been blocked")
                        .build();
            }

            // Get post count
            Long postCount = postRepo.countByOwnerId(userId);

            // Get total likes received
            Long totalLikes = postLikeRepo.countLikesByUserId(userId);

            // Get follow stats
            Long followersCount = followRepository.countByFollowingId(userId);
            Long followingCount = followRepository.countByFollowerId(userId);

            // Get current user's follow status with this user
            Boolean isFollowing = false;
            Boolean isFollowedBy = false;

            try {
                User currentUser = getCurrentUser();
                isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUser.getId(), userId);
                isFollowedBy = followRepository.existsByFollowerIdAndFollowingId(userId, currentUser.getId());
            } catch (Exception e) {
                log.debug("No authenticated user for follow status check");
            }

            // Create stats response
            UserStatsDto stats = UserStatsDto.builder()
                    .userId(userId)
                    .postCount(postCount)
                    .totalLikes(totalLikes)
                    .followersCount(followersCount)
                    .followingCount(followingCount)
                    .isFollowing(isFollowing)
                    .isFollowedBy(isFollowedBy)
                    .build();

            log.info("Successfully fetched stats for user: {}", user.getEmail());
            return UserResponse.builder()
                    .status(200)
                    .message("User stats retrieved successfully")
                    .userStats(stats)
                    .build();

        } catch (NotFoundException e) {
            log.error("User not found with ID: {}", userId);
            return UserResponse.builder()
                    .status(404)
                    .message("User not found")
                    .build();
        } catch (Exception e) {
            log.error("Error fetching user stats for ID {}: {}", userId, e.getMessage(), e);
            return UserResponse.builder()
                    .status(500)
                    .message("Failed to fetch user stats")
                    .build();
        }
    }
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }


    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepo.findByEmail(email).orElse(null);
            return user != null ? user.getId() : null;
        }
        return null;
    }
}

