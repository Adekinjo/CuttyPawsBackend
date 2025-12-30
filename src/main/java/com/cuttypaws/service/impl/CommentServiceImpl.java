package com.cuttypaws.service.impl;

import com.cuttypaws.dto.CommentDto;
import com.cuttypaws.dto.CommentRequestDto;
import com.cuttypaws.entity.Comment;
import com.cuttypaws.entity.Post;
import com.cuttypaws.entity.User;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.exception.UnauthorizedException;
import com.cuttypaws.repository.CommentRepo;
import com.cuttypaws.repository.PostRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.CommentResponse;
import com.cuttypaws.service.interf.CommentLikeService;
import com.cuttypaws.service.interf.CommentService;
import com.cuttypaws.service.interf.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepo commentRepo;
    private final CommentLikeService commentLikeService;
    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;


    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE_TIME;

    // =========================================================================
    // 🚀 CREATE COMMENT / REPLY (real-time)
    // =========================================================================
    @Override
    @Transactional
    public CommentResponse createComment(Long userId, CommentRequestDto request) {
        try {
            if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                return CommentResponse.builder().status(400).message("Comment content is required").build();
            }
            if (request.getPostId() == null) {
                return CommentResponse.builder().status(400).message("Post ID is required").build();
            }

            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            Post post = postRepo.findById(request.getPostId())
                    .orElseThrow(() -> new NotFoundException("Post not found"));

            Comment.CommentBuilder builder = Comment.builder()
                    .content(request.getContent().trim())
                    .user(user)
                    .post(post)
                    .likeCount(0);

            // Reply (1-level deep)
            if (request.getParentCommentId() != null) {
                Comment parent = commentRepo.findById(request.getParentCommentId())
                        .orElseThrow(() -> new NotFoundException("Parent comment not found"));
                builder.parentComment(parent);
            }

            Comment saved = commentRepo.save(builder.build());

            // notify post owner
            notificationService.notifyCommented(post, saved, user);

            // if this is a reply, also notify parent comment owner
            if (saved.getParentComment() != null) {
                notificationService.notifyReply(saved.getParentComment(), saved, user);
            }

            CommentDto dto = mapCommentToDtoWithReplies(saved, userId);

            // 🔥 REAL-TIME broadcast new comment
            messagingTemplate.convertAndSend(
                    "/topic/post/" + post.getId() + "/comments",
                    dto
            );

            // update total comments count realtime too (optional but nice)
            long total = commentRepo.countByPostId(post.getId());
            messagingTemplate.convertAndSend(
                    "/topic/post/" + post.getId() + "/comments-count",
                    total
            );

            return CommentResponse.builder()
                    .status(200)
                    .message("Comment added")
                    .comment(dto)
                    .totalComments((int) total)
                    .build();

        } catch (Exception e) {
            log.error("❌ createComment error: {}", e.getMessage(), e);
            return CommentResponse.builder().status(500).message("Failed to create comment").build();
        }
    }

    // =========================================================================
    // ✏️ UPDATE COMMENT
    // =========================================================================
    @Override
    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentRequestDto request) {
        try {
            Comment comment = commentRepo.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("Comment not found"));

            if (!comment.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("You can only edit your own comment");
            }

            if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
                comment.setContent(request.getContent().trim());
            }

            Comment saved = commentRepo.save(comment);
            CommentDto dto = mapCommentToDto(saved, userId);

            // real-time update
            messagingTemplate.convertAndSend(
                    "/topic/comment/" + commentId + "/updated",
                    dto
            );

            return CommentResponse.builder()
                    .status(200)
                    .message("Comment updated")
                    .comment(dto)
                    .build();

        } catch (UnauthorizedException e) {
            return CommentResponse.builder().status(403).message(e.getMessage()).build();
        } catch (Exception e) {
            log.error("❌ updateComment error: {}", e.getMessage(), e);
            return CommentResponse.builder().status(500).message("Failed to update comment").build();
        }
    }

    // =========================================================================
    // 🗑️ DELETE COMMENT (real-time)
    // =========================================================================
    @Override
    @Transactional
    public CommentResponse deleteComment(Long userId, Long commentId) {
        try {
            Comment comment = commentRepo.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("Comment not found"));

            if (!comment.getUser().getId().equals(userId)) {
                throw new UnauthorizedException("You can only delete your own comment");
            }

            Long postId = comment.getPost().getId();
            commentRepo.delete(comment);

            // real-time delete event
            messagingTemplate.convertAndSend(
                    "/topic/post/" + postId + "/comment-deleted",
                    commentId
            );

            long total = commentRepo.countByPostId(postId);
            messagingTemplate.convertAndSend(
                    "/topic/post/" + postId + "/comments-count",
                    total
            );

            return CommentResponse.builder()
                    .status(200)
                    .message("Comment deleted")
                    .totalComments((int) total)
                    .build();

        } catch (UnauthorizedException e) {
            return CommentResponse.builder().status(403).message(e.getMessage()).build();
        } catch (Exception e) {
            log.error("❌ deleteComment error: {}", e.getMessage(), e);
            return CommentResponse.builder().status(500).message("Failed to delete comment").build();
        }
    }

    // =========================================================================
    // 📌 GET SINGLE COMMENT
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentById(Long commentId) {
        try {
            Comment c = commentRepo.findById(commentId)
                    .orElseThrow(() -> new NotFoundException("Comment not found"));

            return CommentResponse.builder()
                    .status(200)
                    .comment(mapCommentToDtoWithReplies(c, null))
                    .build();

        } catch (Exception e) {
            return CommentResponse.builder().status(500).message("Failed").build();
        }
    }

    // =========================================================================
    // 📄 PAGINATED TOP-LEVEL COMMENTS (Instagram style)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public CommentResponse getCommentsByPostId(Long postId, Long currentUserId, int page, int size) {
        try {
            if (!postRepo.existsById(postId)) {
                return CommentResponse.builder().status(404).message("Post not found").build();
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<Comment> result = commentRepo.findTopLevelComments(postId, pageable);

            List<CommentDto> dtos = result.getContent().stream()
                    .map(c -> mapCommentToDtoWithReplies(c, currentUserId))
                    .collect(Collectors.toList());

            long total = commentRepo.countByPostId(postId);

            return CommentResponse.builder()
                    .status(200)
                    .commentList(dtos)
                    .totalComments((int) total)
                    .currentPage(result.getNumber())
                    .totalPages(result.getTotalPages())
                    .build();

        } catch (Exception e) {
            log.error("❌ getComments error: {}", e.getMessage(), e);
            return CommentResponse.builder().status(500).message("Failed to fetch comments").build();
        }
    }

    // =========================================================================
// ✅ MAPPERS
// =========================================================================
    private CommentDto mapCommentToDto(Comment c, Long currentUserId) {
        if (c == null) return null;

        try {
            // Get reaction data for this comment
            Map<String, Object> reactionData = getReactionData(c.getId());
            Map<String, Integer> reactionsCountInt = (Map<String, Integer>) reactionData.get("counts");

            // Convert Map<String, Integer> to Map<String, Long>
            Map<String, Long> reactionsCount = reactionsCountInt.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().longValue()
                    ));

            int totalReactions = (int) reactionData.get("total");

            // Check if current user has reacted to this comment
            boolean isLikedByCurrentUser = false;
            String userReactionType = null;

            if (currentUserId != null) {
                CommentResponse userReactionResponse = commentLikeService.checkUserReaction(currentUserId, c.getId());
                if (userReactionResponse.getStatus() == 200 && userReactionResponse.getData() != null) {
                    Map<String, Object> userReactionData = (Map<String, Object>) userReactionResponse.getData();
                    isLikedByCurrentUser = (boolean) userReactionData.get("hasReacted");
                    if (userReactionData.containsKey("reactionType")) {
                        userReactionType = userReactionData.get("reactionType").toString();
                    }
                }
            }

            return CommentDto.builder()
                    .id(c.getId())
                    .content(c.getContent())
                    .userId(c.getUser().getId())
                    .userName(c.getUser().getName())
                    .userProfileImage(c.getUser().getProfileImageUrl())
                    .postId(c.getPost().getId())
                    .parentCommentId(c.getParentComment() != null ? c.getParentComment().getId() : null)
                    .likeCount(totalReactions) // Use total reactions count instead of old likeCount
                    .isLikedByCurrentUser(isLikedByCurrentUser)
                    .reactions(reactionsCount) // Map<String, Long> - reaction types and their counts
                    .userReaction(userReactionType) // Current user's reaction type
                    .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().format(ISO) : null)
                    .updatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().format(ISO) : null)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error mapping comment {} to DTO: {}", c.getId(), e.getMessage());
            // Return basic DTO without reaction data if there's an error
            return CommentDto.builder()
                    .id(c.getId())
                    .content(c.getContent())
                    .userId(c.getUser().getId())
                    .userName(c.getUser().getName())
                    .userProfileImage(c.getUser().getProfileImageUrl())
                    .postId(c.getPost().getId())
                    .parentCommentId(c.getParentComment() != null ? c.getParentComment().getId() : null)
                    .likeCount(0)
                    .isLikedByCurrentUser(false)
                    .reactions(new HashMap<>()) // Empty Map<String, Long>
                    .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().format(ISO) : null)
                    .updatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().format(ISO) : null)
                    .build();
        }
    }

    private CommentDto mapCommentToDtoWithReplies(Comment comment, Long currentUserId) {
        CommentDto dto = mapCommentToDto(comment, currentUserId);

        // 1-level replies only
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            List<CommentDto> replyDtos = comment.getReplies().stream()
                    .sorted(Comparator.comparing(Comment::getCreatedAt)) // oldest first like IG
                    .map(r -> mapCommentToDto(r, currentUserId))
                    .collect(Collectors.toList());
            dto.setReplies(replyDtos);
        }

        return dto;
    }

    private Map<String, Object> getReactionData(Long commentId) {
        try {
            CommentResponse reactionResponse = commentLikeService.getCommentReactions(commentId);
            if (reactionResponse.getStatus() == 200 && reactionResponse.getData() != null) {
                return (Map<String, Object>) reactionResponse.getData();
            }
        } catch (Exception e) {
            log.error("❌ Error getting reaction data for comment {}: {}", commentId, e.getMessage());
        }

        // Return empty reaction data if there's an error
        Map<String, Object> emptyReactionData = new HashMap<>();
        emptyReactionData.put("counts", new HashMap<String, Integer>());
        emptyReactionData.put("total", 0);
        emptyReactionData.put("commentId", commentId);
        return emptyReactionData;
    }


}
