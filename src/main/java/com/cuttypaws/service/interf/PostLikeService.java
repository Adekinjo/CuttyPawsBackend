package com.cuttypaws.service.interf;


import com.cuttypaws.entity.PostLike;
import com.cuttypaws.response.PostLikeResponse;

import java.util.UUID;

public interface PostLikeService {

    // New reaction methods
    PostLikeResponse reactToPost(UUID userId, Long postId, PostLike.ReactionType reactionType);
    PostLikeResponse removeReaction(UUID userId, Long postId);
    PostLikeResponse getPostReactions(Long postId);
    PostLikeResponse checkUserReaction(UUID userId, Long postId);

    // Keep existing methods for backward compatibility
    PostLikeResponse likePost(UUID userId, Long postId);
    PostLikeResponse unlikePost(UUID userId, Long postId);
    PostLikeResponse getPostLikes(Long postId);
    PostLikeResponse getUserLikedPosts(UUID userId);
    PostLikeResponse checkIfUserLikedPost(UUID userId, Long postId);
}