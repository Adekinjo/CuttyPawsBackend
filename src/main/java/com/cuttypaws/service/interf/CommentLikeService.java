package com.cuttypaws.service.interf;


import com.cuttypaws.entity.CommentLike;
import com.cuttypaws.response.CommentResponse;

public interface CommentLikeService {
    CommentResponse reactToComment(Long userId, Long commentId, CommentLike.ReactionType reactionType);
    CommentResponse removeReaction(Long userId, Long commentId);
    CommentResponse getCommentReactions(Long commentId);
    CommentResponse checkUserReaction(Long userId, Long commentId);
    CommentResponse likeComment(Long userId, Long commentId);
    CommentResponse unlikeComment(Long userId, Long commentId);
}

