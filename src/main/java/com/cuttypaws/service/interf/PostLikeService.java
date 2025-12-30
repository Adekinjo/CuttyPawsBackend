package com.cuttypaws.service.interf;


import com.cuttypaws.entity.PostLike;
import com.cuttypaws.response.PostLikeResponse;

public interface PostLikeService {

    // New reaction methods
    PostLikeResponse reactToPost(Long userId, Long postId, PostLike.ReactionType reactionType);
    PostLikeResponse removeReaction(Long userId, Long postId);
    PostLikeResponse getPostReactions(Long postId);
    PostLikeResponse checkUserReaction(Long userId, Long postId);

    // Keep existing methods for backward compatibility
    PostLikeResponse likePost(Long userId, Long postId);
    PostLikeResponse unlikePost(Long userId, Long postId);
    PostLikeResponse getPostLikes(Long postId);
    PostLikeResponse getUserLikedPosts(Long userId);
    PostLikeResponse checkIfUserLikedPost(Long userId, Long postId);
}