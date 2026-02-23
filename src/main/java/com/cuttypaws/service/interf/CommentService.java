package com.cuttypaws.service.interf;


import com.cuttypaws.dto.CommentRequestDto;
import com.cuttypaws.response.CommentResponse;

import java.util.UUID;

public interface CommentService {

    CommentResponse createComment(UUID userId, CommentRequestDto request);

    CommentResponse updateComment(UUID userId, Long commentId, CommentRequestDto request);

    CommentResponse deleteComment(UUID userId, Long commentId);

    CommentResponse getCommentsByPostId(Long postId, UUID currentUserId, int page, int size);

    CommentResponse getCommentById(Long commentId);

}
