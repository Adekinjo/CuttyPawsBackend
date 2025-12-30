package com.cuttypaws.service.interf;


import com.cuttypaws.dto.CommentRequestDto;
import com.cuttypaws.response.CommentResponse;

public interface CommentService {

    CommentResponse createComment(Long userId, CommentRequestDto request);

    CommentResponse updateComment(Long userId, Long commentId, CommentRequestDto request);

    CommentResponse deleteComment(Long userId, Long commentId);

    CommentResponse getCommentsByPostId(Long postId, Long currentUserId, int page, int size);

    CommentResponse getCommentById(Long commentId);

}
