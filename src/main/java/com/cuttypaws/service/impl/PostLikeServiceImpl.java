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
    public PostLikeResponse reactToPost(UUID userId, Long postId, PostLike.ReactionType reactionType) {
        try {
            log.info("Reacting to post - User: {}, Post: {}, Reaction: {}", userId, postId, reactionType);

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            Post post = postRepo.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found"));

            Optional<PostLike> existingReaction = postLikeRepo.findByUserIdAndPostId(userId, postId);

            if (existingReaction.isPresent()) {
                // Update existing reaction
                PostLike reaction = existingReaction.get();
                reaction.setReactionType(reactionType);
                postLikeRepo.save(reaction);
                log.info("Reaction updated to: {}", reactionType);
            } else {
                // Create new reaction
                PostLike postLike = PostLike.builder()
                        .user(user)
                        .post(post)
                        .reactionType(reactionType)
                        .build();
                postLikeRepo.save(postLike);
                log.info("New reaction created: {}", reactionType);
            }

            // Get reaction counts for this post
            Map<String, Object> reactionData = getReactionData(postId);
            PostDto postDto = mapper.mapPostToDto(post, userId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("post", postDto);
            responseData.put("reactions", reactionData);
            responseData.put("userReaction", reactionType);

            // Send notification
            if (!userId.equals(post.getOwner().getId())) {
                notificationService.notifyPostLiked(post, user);
            }

            return PostLikeResponse.builder()
                    .status(200)
                    .message("Reaction added successfully")
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
            Optional<PostLike> existingReaction = postLikeRepo.findByUserIdAndPostId(userId, postId);

            if (existingReaction.isEmpty()) {
                return PostLikeResponse.builder()
                        .status(400)
                        .message("You have not reacted to this post")
                        .build();
            }

            postLikeRepo.delete(existingReaction.get());

            Map<String, Object> reactionData = getReactionData(postId);
            Post post = postRepo.findById(postId).orElseThrow(() -> new NotFoundException("Post not found"));
            PostDto postDto = mapper.mapPostToDto(post, userId);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("post", postDto);
            responseData.put("reactions", reactionData);

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
        List<Object[]> reactionCounts = postLikeRepo.getReactionCountsByPostId(postId);
        Map<String, Integer> reactions = new HashMap<>();
        int totalReactions = 0;

        for (Object[] result : reactionCounts) {
            PostLike.ReactionType type = (PostLike.ReactionType) result[0];
            Long count = (Long) result[1];
            reactions.put(type.name(), count.intValue());
            totalReactions += count.intValue();
        }

        Map<String, Object> reactionData = new HashMap<>();
        reactionData.put("counts", reactions);
        reactionData.put("total", totalReactions);
        reactionData.put("postId", postId);

        return reactionData;
    }

    // Existing methods for backward compatibility
    @Override
    public PostLikeResponse likePost(UUID userId, Long postId) {
        return reactToPost(userId, postId, PostLike.ReactionType.LIKE);
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