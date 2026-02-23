package com.cuttypaws.service.impl;

import com.cuttypaws.entity.Comment;
import com.cuttypaws.entity.CommentLike;
import com.cuttypaws.entity.User;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.repository.CommentLikeRepo;
import com.cuttypaws.repository.CommentRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.CommentResponse;
import com.cuttypaws.service.interf.CommentLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentLikeRepo commentLikeRepo;
    private final CommentRepo commentRepo;
    private final UserRepo userRepo;

    @Override
    @Transactional
    public CommentResponse reactToComment(UUID userId, Long commentId, CommentLike.ReactionType reactionType) {
        try {
            log.info("Reacting to comment - User: {}, Comment: {}, Reaction: {}", userId, commentId, reactionType);

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            Comment comment = commentRepo.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("Comment not found"));

            Optional<CommentLike> existingReaction = commentLikeRepo.findByUserIdAndCommentId(userId, commentId);

            if (existingReaction.isPresent()) {
                // Update existing reaction
                CommentLike reaction = existingReaction.get();
                reaction.setReactionType(reactionType);
                commentLikeRepo.save(reaction);
                log.info("Comment reaction updated to: {}", reactionType);
            } else {
                // Create new reaction
                CommentLike commentLike = CommentLike.builder()
                        .user(user)
                        .comment(comment)
                        .reactionType(reactionType)
                        .build();
                commentLikeRepo.save(commentLike);
                log.info("New comment reaction created: {}", reactionType);
            }

            Map<String, Object> reactionData = getReactionData(commentId);

            return CommentResponse.builder()
                    .status(200)
                    .message("Reaction added successfully")
                    .data(reactionData)
                    .build();

        } catch (NotFoundException e) {
            return CommentResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error reacting to comment: {}", e.getMessage(), e);
            return CommentResponse.builder()
                    .status(500)
                    .message("Failed to react to comment")
                    .build();
        }
    }

    @Override
    @Transactional
    public CommentResponse removeReaction(UUID userId, Long commentId) {
        try {
            Optional<CommentLike> existingReaction = commentLikeRepo.findByUserIdAndCommentId(userId, commentId);

            if (existingReaction.isEmpty()) {
                return CommentResponse.builder()
                        .status(400)
                        .message("You have not reacted to this comment")
                        .build();
            }

            commentLikeRepo.delete(existingReaction.get());

            Map<String, Object> reactionData = getReactionData(commentId);

            return CommentResponse.builder()
                    .status(200)
                    .message("Reaction removed successfully")
                    .data(reactionData)
                    .build();

        } catch (Exception e) {
            log.error("Error removing comment reaction: {}", e.getMessage(), e);
            return CommentResponse.builder()
                    .status(500)
                    .message("Failed to remove reaction")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentReactions(Long commentId) {
        try {
            if (!commentRepo.existsById(commentId)) {
                throw new NotFoundException("Comment not found");
            }

            Map<String, Object> reactionData = getReactionData(commentId);

            return CommentResponse.builder()
                    .status(200)
                    .message("Reactions retrieved successfully")
                    .data(reactionData)
                    .build();

        } catch (NotFoundException e) {
            return CommentResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error getting comment reactions: {}", e.getMessage(), e);
            return CommentResponse.builder()
                    .status(500)
                    .message("Failed to get comment reactions")
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CommentResponse checkUserReaction(UUID userId, Long commentId) {
        try {
            Optional<CommentLike> userReaction = commentLikeRepo.findByUserIdAndCommentId(userId, commentId);

            Map<String, Object> data = new HashMap<>();
            data.put("hasReacted", userReaction.isPresent());
            if (userReaction.isPresent()) {
                data.put("reactionType", userReaction.get().getReactionType());
            }

            return CommentResponse.builder()
                    .status(200)
                    .message("User reaction status retrieved")
                    .data(data)
                    .build();

        } catch (Exception e) {
            log.error("Error checking user reaction: {}", e.getMessage(), e);
            return CommentResponse.builder()
                    .status(500)
                    .message("Failed to check user reaction")
                    .build();
        }
    }

    private Map<String, Object> getReactionData(Long commentId) {
        List<Object[]> reactionCounts = commentLikeRepo.getReactionCountsByCommentId(commentId);
        Map<String, Integer> reactions = new HashMap<>();
        int totalReactions = 0;

        for (Object[] result : reactionCounts) {
            CommentLike.ReactionType type = (CommentLike.ReactionType) result[0];
            Long count = (Long) result[1];
            reactions.put(type.name(), count.intValue());
            totalReactions += count.intValue();
        }

        Map<String, Object> reactionData = new HashMap<>();
        reactionData.put("counts", reactions);
        reactionData.put("total", totalReactions);
        reactionData.put("commentId", commentId);

        return reactionData;
    }

    // Simple like/unlike for backward compatibility
    @Override
    public CommentResponse likeComment(UUID userId, Long commentId) {
        return reactToComment(userId, commentId, CommentLike.ReactionType.LIKE);
    }

    @Override
    public CommentResponse unlikeComment(UUID userId, Long commentId) {
        return removeReaction(userId, commentId);
    }
}