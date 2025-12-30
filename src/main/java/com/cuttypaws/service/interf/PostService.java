package com.cuttypaws.service.interf;


import com.cuttypaws.dto.PostRequestDto;
import com.cuttypaws.response.PostResponse;

public interface PostService {

    PostResponse createPost(Long userId, PostRequestDto request);

    PostResponse updatePost(Long userId, Long postId, PostRequestDto request); // Added userId for security

    PostResponse deletePost(Long userId, Long postId); // Added userId for security

    PostResponse getPostById(Long postId);

    PostResponse getMyPosts(Long userId, Long currentUserId);

    PostResponse getAllPosts(Long currentUserId);

    PostResponse getUserPosts(Long userId, Long currentUserId); // Get posts by specific user
}



