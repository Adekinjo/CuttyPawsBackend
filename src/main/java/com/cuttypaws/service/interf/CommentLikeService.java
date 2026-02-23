package com.cuttypaws.service.interf;


import com.cuttypaws.entity.CommentLike;
import com.cuttypaws.response.CommentResponse;

import java.util.UUID;

public interface CommentLikeService {
    CommentResponse reactToComment(UUID userId, Long commentId, CommentLike.ReactionType reactionType);
    CommentResponse removeReaction(UUID userId, Long commentId);
    CommentResponse getCommentReactions(Long commentId);
    CommentResponse checkUserReaction(UUID userId, Long commentId);
    CommentResponse likeComment(UUID userId, Long commentId);
    CommentResponse unlikeComment(UUID userId, Long commentId);
}

