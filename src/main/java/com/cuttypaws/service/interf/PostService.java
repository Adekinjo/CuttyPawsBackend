package com.cuttypaws.service.interf;


import com.cuttypaws.dto.PostRequestDto;
import com.cuttypaws.response.PostResponse;

import java.util.UUID;

public interface PostService {

    PostResponse createPost(UUID userId, PostRequestDto request);

    PostResponse updatePost(UUID userId, Long postId, PostRequestDto request); // Added userId for security

    PostResponse deletePost(UUID userId, Long postId); // Added userId for security

    PostResponse getPostById(Long postId);

    PostResponse getMyPosts(UUID userId, UUID currentUserId);

    PostResponse getAllPosts();

    PostResponse getUserPosts(UUID userId, UUID currentUserId); // Get posts by specific user
}


