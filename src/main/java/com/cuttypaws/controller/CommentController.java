package com.cuttypaws.controller;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.CommentLike;
import com.cuttypaws.response.*;
import com.cuttypaws.security.CurrentUser;
import com.cuttypaws.service.interf.CommentLikeService;
import com.cuttypaws.service.interf.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;

    // =========================================================================
    //  CREATE COMMENT / REPLY
    //  POST /comments/create
    // =========================================================================
    @PostMapping("/create")
    public ResponseEntity<CommentResponse> createComment(
            @CurrentUser Long userId,
            @RequestBody CommentRequestDto request
    ) {
        log.info("📝 Create comment - user: {}, post: {}", userId, request.getPostId());
        CommentResponse res = commentService.createComment(userId, request);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  UPDATE COMMENT
    //  PUT /comments/{commentId}
    // =========================================================================
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @CurrentUser Long userId,
            @PathVariable Long commentId,
            @RequestBody CommentRequestDto request
    ) {
        log.info("✏️ Update comment {} by user {}", commentId, userId);
        CommentResponse res = commentService.updateComment(userId, commentId, request);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  DELETE COMMENT
    //  DELETE /comments/{commentId}
    // =========================================================================
    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentResponse> deleteComment(
            @CurrentUser Long userId,
            @PathVariable Long commentId
    ) {
        log.info("🗑️ Delete comment {} by user {}", commentId, userId);
        CommentResponse res = commentService.deleteComment(userId, commentId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  GET PAGINATED POST COMMENTS
    //  GET /comments/post/{postId}?page=0&size=20
    // =========================================================================
    @GetMapping("/post/{postId}")
    public ResponseEntity<CommentResponse> getCommentsForPost(
            @PathVariable Long postId,
            @CurrentUser Long currentUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("📄 Fetch comments for post {}", postId);
        CommentResponse res = commentService.getCommentsByPostId(postId, currentUserId, page, size);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  GET SINGLE COMMENT
    //  GET /comments/{commentId}
    // =========================================================================
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponse> getComment(
            @PathVariable Long commentId
    ) {
        log.info("🔍 Fetch comment {}", commentId);
        CommentResponse res = commentService.getCommentById(commentId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  SIMPLE LIKE/UNLIKE COMMENT (using PostLike pattern)
    // =========================================================================
    @PostMapping("/{commentId}/like")
    public ResponseEntity<CommentResponse> likeComment(
            @CurrentUser Long userId,
            @PathVariable Long commentId
    ) {
        log.info("❤️ Like comment {} by user {}", commentId, userId);
        CommentResponse res = commentLikeService.likeComment(userId, commentId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/{commentId}/unlike")
    public ResponseEntity<CommentResponse> unlikeComment(
            @CurrentUser Long userId,
            @PathVariable Long commentId
    ) {
        log.info("💔 Unlike comment {} by user {}", commentId, userId);
        CommentResponse res = commentLikeService.unlikeComment(userId, commentId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  ADVANCED REACTIONS (using PostLike pattern)
    // =========================================================================
    @PostMapping("/{commentId}/react")
    public ResponseEntity<CommentResponse> reactToComment(
            @CurrentUser Long userId,
            @PathVariable Long commentId,
            @RequestParam CommentLike.ReactionType reaction
    ) {
        log.info("🎭 React '{}' to comment {} by user {}", reaction, commentId, userId);
        CommentResponse res = commentLikeService.reactToComment(userId, commentId, reaction);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/{commentId}/remove-reaction")
    public ResponseEntity<CommentResponse> removeReaction(
            @CurrentUser Long userId,
            @PathVariable Long commentId
    ) {
        log.info("🚫 Remove reaction from comment {} by user {}", commentId, userId);
        CommentResponse res = commentLikeService.removeReaction(userId, commentId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    // =========================================================================
    //  GET REACTIONS DATA
    // =========================================================================
    @GetMapping("/{commentId}/reactions")
    public ResponseEntity<CommentResponse> getCommentReactions(@PathVariable Long commentId) {
        log.info("📊 Get reactions for comment {}", commentId);
        CommentResponse res = commentLikeService.getCommentReactions(commentId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/{commentId}/user-reaction")
    public ResponseEntity<CommentResponse> getUserReaction(
            @CurrentUser Long userId,
            @PathVariable Long commentId
    ) {
        log.info("🔍 Get user reaction for comment {} by user {}", commentId, userId);
        CommentResponse res = commentLikeService.checkUserReaction(userId, commentId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}