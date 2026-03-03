package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.exception.*;
import com.cuttypaws.mapper.PostMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.*;
import com.cuttypaws.service.interf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostLikeServiceImpl implements PostLikeService {

    private final PostLikeRepo postLikeRepo;
    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final PostMapper mapper;
    private final NotificationService notificationService;


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "postReactions", key = "#postId"),
            @CacheEvict(value = "userPostReaction", key = "T(String).valueOf(#userId).concat(':').concat(T(String).valueOf(#postId))"),
            @CacheEvict(value = "userLikedPosts", key = "#userId"),
            @CacheEvict(value = "postById", key = "#postId"),
            @CacheEvict(value = "postsAll", allEntries = true),
            @CacheEvict(value = "postsByUser", allEntries = true)
    })
    public PostLikeResponse reactToPost(UUID userId, Long postId, PostLike.ReactionType reactionType) {
        try {
            if (reactionType == null) {
                return PostLikeResponse.builder()
                        .status(400)
                        .message("Reaction type is required")
                        .build();
            }

            // Validate existence quickly
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            Post post = postRepo.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found"));

            Optional<PostLike> existing = postLikeRepo.findByUserIdAndPostId(userId, postId);

            PostLike.ReactionType previousReaction = null;

            if (existing.isPresent()) {
                PostLike entity = existing.get();
                previousReaction = entity.getReactionType();

                // Same reaction -> just return counts (still fast)
                if (previousReaction == reactionType) {
                    Map<String, Object> reactionData = getReactionData(postId);

                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("reactions", reactionData);
                    responseData.put("userReaction", previousReaction);
                    responseData.put("previousReaction", previousReaction);

                    return PostLikeResponse.builder()
                            .status(200)
                            .message("Reaction unchanged")
                            .data(responseData)
                            .build();
                }

                entity.setReactionType(reactionType);
                postLikeRepo.save(entity);

            } else {
                PostLike created = PostLike.builder()
                        .user(user)
                        .post(post)
                        .reactionType(reactionType)
                        .build();
                postLikeRepo.save(created);
            }

            // Return updated counts (single source of truth)
            Map<String, Object> reactionData = getReactionData(postId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("reactions", reactionData);
            responseData.put("userReaction", reactionType);
            responseData.put("previousReaction", previousReaction);

            // Send notification (only if reacting to someone else's post)
            if (post.getOwner() != null && !userId.equals(post.getOwner().getId())) {
                notificationService.notifyPostLiked(post, user); // rename to notifyPostReacted later
            }

            return PostLikeResponse.builder()
                    .status(200)
                    .message("Reaction saved successfully")
                    .data(responseData)
                    .build();

        } catch (NotFoundException e) {
            return PostLikeResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error reacting to post: {}", e.getMessage(), e);
            return PostLikeResponse.builder()
                    .status(500)
                    .message("Failed to react to post")
                    .build();
        }
    }

    @Override
    @Transactional
    public PostLikeResponse removeReaction(UUID userId, Long postId) {
        try {
            Optional<PostLike> existing = postLikeRepo.findByUserIdAndPostId(userId, postId);

            if (existing.isEmpty()) {
                // Still return counts for UI stability
                Map<String, Object> reactionData = getReactionData(postId);

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("reactions", reactionData);
                responseData.put("userReaction", null);

                return PostLikeResponse.builder()
                        .status(200)
                        .message("No reaction to remove")
                        .data(responseData)
                        .build();
            }

            postLikeRepo.delete(existing.get());

            Map<String, Object> reactionData = getReactionData(postId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("reactions", reactionData);
            responseData.put("userReaction", null);

            return PostLikeResponse.builder()
                    .status(200)
                    .message("Reaction removed successfully")
                    .data(responseData)
                    .build();

        } catch (Exception e) {
            log.error("Error removing reaction: {}", e.getMessage(), e);
            return PostLikeResponse.builder()
                    .status(500)
                    .message("Failed to remove reaction")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "postReactions",
            key = "#postId",
            condition = "@cacheToggleService.isEnabled()",
            unless = "#result == null || #result.data == null"
    )
    public PostLikeResponse getPostReactions(Long postId) {
        try {
            if (!postRepo.existsById(postId)) {
                throw new NotFoundException("Post not found");
            }

            Map<String, Object> reactionData = getReactionData(postId);

            return PostLikeResponse.builder()
                    .status(200)
                    .message("Reactions retrieved successfully")
                    .data(reactionData)
                    .build();

        } catch (NotFoundException e) {
            return PostLikeResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error getting post reactions: {}", e.getMessage(), e);
            return PostLikeResponse.builder()
                    .status(500)
                    .message("Failed to get post reactions")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "userPostReaction",
            key = "T(String).valueOf(#userId).concat(':').concat(T(String).valueOf(#postId))",
            condition = "@cacheToggleService.isEnabled()",
            unless = "#result == null || #result.data == null"
    )
    public PostLikeResponse checkUserReaction(UUID userId, Long postId) {
        try {
            Optional<PostLike> userReaction = postLikeRepo.findByUserIdAndPostId(userId, postId);

            Map<String, Object> data = new HashMap<>();
            data.put("hasReacted", userReaction.isPresent());
            if (userReaction.isPresent()) {
                data.put("reactionType", userReaction.get().getReactionType());
            }

            return PostLikeResponse.builder()
                    .status(200)
                    .message("User reaction status retrieved")
                    .data(data)
                    .build();

        } catch (Exception e) {
            log.error("Error checking user reaction: {}", e.getMessage(), e);
            return PostLikeResponse.builder()
                    .status(500)
                    .message("Failed to check user reaction")
                    .build();
        }
    }

    private Map<String, Object> getReactionData(Long postId) {
        List<Object[]> rows = postLikeRepo.getReactionCountsByPostId(postId);

        // Defaults for all reaction types
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PostLike.ReactionType t : PostLike.ReactionType.values()) {
            counts.put(t.name(), 0);
        }

        int total = 0;
        for (Object[] row : rows) {
            PostLike.ReactionType type = (PostLike.ReactionType) row[0];
            Long c = (Long) row[1];

            int value = (c == null) ? 0 : c.intValue();
            counts.put(type.name(), value);
            total += value;
        }

        Map<String, Object> reactionData = new HashMap<>();
        reactionData.put("postId", postId);
        reactionData.put("counts", counts);
        reactionData.put("total", total);

        return reactionData;
    }

    // Existing methods for backward compatibility
    @Override
    public PostLikeResponse likePost(UUID userId, Long postId) {
        return reactToPost(userId, postId, PostLike.ReactionType.PAWPRINT);
    }

    @Override
    public PostLikeResponse unlikePost(UUID userId, Long postId) {
        return removeReaction(userId, postId);
    }

    @Override
    public PostLikeResponse getPostLikes(Long postId) {
        return getPostReactions(postId);
    }

    @Override
    @Cacheable(
            value = "userLikedPosts",
            key = "#userId",
            condition = "@cacheToggleService.isEnabled()",
            unless = "#result == null || #result.postList == null"
    )
    public PostLikeResponse getUserLikedPosts(UUID userId) {
        try {
            List<PostLike> userLikes = postLikeRepo.findByUserId(userId);
            List<PostDto> likedPosts = userLikes.stream()
                    .map(PostLike::getPost)
                    .map(post -> mapper.mapPostToDto(post, userId))
                    .collect(Collectors.toList());

            return PostLikeResponse.builder()
                    .status(200)
                    .message("Liked posts retrieved successfully")
                    .postList(likedPosts)
                    .build();

        } catch (Exception e) {
            log.error("Error getting liked posts: {}", e.getMessage(), e);
            return PostLikeResponse.builder()
                    .status(500)
                    .message("Failed to get liked posts")
                    .build();
        }
    }

    @Override
    public PostLikeResponse checkIfUserLikedPost(UUID userId, Long postId) {
        return checkUserReaction(userId, postId);
    }
}