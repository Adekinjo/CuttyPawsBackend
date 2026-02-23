package com.cuttypaws.controller;

import com.cuttypaws.entity.PostLike;
import com.cuttypaws.response.PostLikeResponse;
import com.cuttypaws.response.PostResponse;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.security.CurrentUser;
import com.cuttypaws.service.interf.PostLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Slf4j
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping("/{postId}/react")
    public ResponseEntity<PostLikeResponse> reactToPost(
            @CurrentUser UUID userId,
            @PathVariable Long postId,
            @RequestParam PostLike.ReactionType reaction
    ) {
        try {
            PostLikeResponse response = postLikeService.reactToPost(userId, postId, reaction);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            log.error("Error reacting to post: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    PostLikeResponse.builder()
                            .status(500)
                            .message("Internal server error")
                            .build()
            );
        }
    }

    @PostMapping("/{postId}/remove-reaction")
    public ResponseEntity<PostLikeResponse> removeReaction(
            @CurrentUser UUID userId,
            @PathVariable Long postId
    ) {
        try {
            PostLikeResponse response = postLikeService.removeReaction(userId, postId);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            log.error("Error removing reaction: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    PostLikeResponse.builder()
                            .status(500)
                            .message("Internal server error")
                            .build()
            );
        }
    }

    @GetMapping("/{postId}/reactions")
    public ResponseEntity<PostLikeResponse> getPostReactions(@PathVariable Long postId) {
        try {
            PostLikeResponse response = postLikeService.getPostReactions(postId);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            log.error("Error getting post reactions: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    PostLikeResponse.builder()
                            .status(500)
                            .message("Internal server error")
                            .build()
            );
        }
    }

    @GetMapping("/{postId}/user-reaction")
    public ResponseEntity<PostLikeResponse> getUserReaction(
            @CurrentUser UUID userId,
            @PathVariable Long postId
    ) {
        try {
            PostLikeResponse response = postLikeService.checkUserReaction(userId, postId);
            return ResponseEntity.status(response.getStatus()).body(response);
        } catch (Exception e) {
            log.error("Error getting user reaction: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    PostLikeResponse.builder()
                            .status(500)
                            .message("Internal server error")
                            .build()
            );
        }
    }

    // Keep existing endpoints for backward compatibility
    @PostMapping("/{postId}/like")
    public ResponseEntity<PostLikeResponse> likePost(
            @CurrentUser UUID userId,
            @PathVariable Long postId
    ) {
        return reactToPost(userId, postId, PostLike.ReactionType.LIKE);
    }

    @PostMapping("/{postId}/unlike")
    public ResponseEntity<PostLikeResponse> unlikePost(
            @CurrentUser UUID userId,
            @PathVariable Long postId
    ) {
        return removeReaction(userId, postId);
    }
}