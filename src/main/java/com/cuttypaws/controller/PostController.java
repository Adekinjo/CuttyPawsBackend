package com.cuttypaws.controller;

import com.cuttypaws.dto.PostRequestDto;
import com.cuttypaws.response.PostResponse;
import com.cuttypaws.security.CurrentUser;
import com.cuttypaws.service.interf.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // ================== CREATE POST ==================
    @PostMapping("/create")
    public ResponseEntity<PostResponse> createPost(
            @CurrentUser UUID currentUserId,
            @ModelAttribute PostRequestDto request
    ) {
        PostResponse response = postService.createPost(currentUserId, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ================== UPDATE POST ==================
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(
            @CurrentUser UUID currentUserId,
            @PathVariable Long postId,
            @ModelAttribute PostRequestDto request
    ) {
        PostResponse response = postService.updatePost(currentUserId, postId, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ================== DELETE POST ==================
    @DeleteMapping("/{postId}")
    public ResponseEntity<PostResponse> deletePost(
            @CurrentUser UUID currentUserId,
            @PathVariable Long postId
    ) {
        PostResponse response = postService.deletePost(currentUserId, postId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ================== GET SINGLE POST ==================
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long postId) {
        PostResponse response = postService.getPostById(postId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ================== GET MY POSTS ==================
    @GetMapping("/my-posts")
    public ResponseEntity<PostResponse> getMyPosts(@CurrentUser() UUID currentUserId) {
        PostResponse response = postService.getMyPosts(currentUserId, currentUserId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ================== GET ALL POSTS FOR FEED ==================
    @GetMapping("/get-all")
    public ResponseEntity<PostResponse> getAllPosts() {
        // Feed must respect logged-in user for "isLikedByCurrentUser"
        PostResponse response = postService.getAllPosts();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    // ================== GET POSTS OF ANOTHER USER ==================
    @GetMapping("/user/{userId}")
    public ResponseEntity<PostResponse> getUserPosts(
            @PathVariable UUID userId,
            @CurrentUser UUID currentUserId
    ) {
        PostResponse response = postService.getUserPosts(userId, currentUserId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}