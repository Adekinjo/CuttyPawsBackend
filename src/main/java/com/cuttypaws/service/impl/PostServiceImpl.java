package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.exception.*;
import com.cuttypaws.mapper.PostMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.response.PostResponse;
import com.cuttypaws.service.AwsS3Service;
import com.cuttypaws.service.interf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final AwsS3Service awsS3Service;
    private final PostMapper mapper;
    private final FollowRepo followRepo;
    private final NotificationService notificationService;


    @Override
    @Transactional
    public PostResponse createPost(Long userId, PostRequestDto request) {
        try {
            log.info("📌 [CREATE POST] Started by userId: {}", userId);

            // Validate input
            if (request.getCaption() == null || request.getCaption().trim().isEmpty()) {
                return PostResponse.builder()
                        .status(400)
                        .message("Caption is required")
                        .build();
            }

            if (request.getImages() == null || request.getImages().isEmpty()) {
                return PostResponse.builder()
                        .status(400)
                        .message("At least one image is required")
                        .build();
            }

            // Find user
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // Create post
            Post post = Post.builder()
                    .caption(request.getCaption().trim())
                    .owner(user)
                    .images(new ArrayList<>())
                    .build();

            // Upload images
            log.info("📤 Uploading {} images...", request.getImages().size());
            List<PostImage> images = request.getImages().stream()
                    .map(file -> {
                        try {
                            String url = awsS3Service.saveImageToS3(file);
                            log.info("✔ Uploaded to: {}", url);
                            return PostImage.builder()
                                    .imageUrl(url)
                                    .post(post)
                                    .build();
                        } catch (Exception e) {
                            log.error("❌ Failed to upload image: {}", e.getMessage());
                            throw new RuntimeException("Failed to upload image: " + file.getOriginalFilename());
                        }
                    })
                    .collect(Collectors.toList());

            post.setImages(images);

            // Save post
            log.info("💾 Saving post...");
            Post savedPost = postRepo.save(post);
            PostDto postDto = mapper.mapPostToDto(savedPost);


            List<Long> followerIds = followRepo.findFollowerIds(userId);
            notificationService.notifyFollowersNewPost(savedPost, user, followerIds);

            log.info("✅ Post created successfully with ID: {}", savedPost.getId());
            return PostResponse.builder()
                    .status(200)
                    .message("Post created successfully")
                    .post(postDto)
                    .build();

        } catch (NotFoundException e) {
            log.error("User not found for post creation: {}", e.getMessage());
            return PostResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("❌ Error creating post: {}", e.getMessage(), e);
            return PostResponse.builder()
                    .status(500)
                    .message("Failed to create post: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public PostResponse updatePost(Long userId, Long postId, PostRequestDto request) {
        try {
            log.info("✏ Updating post with ID: {} by user: {}", postId, userId);

            // Find post
            Post post = postRepo.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found"));

            // Check ownership
            if (!post.getOwner().getId().equals(userId)) {
                throw new UnauthorizedException("You can only update your own posts");
            }

            // Update caption
            if (request.getCaption() != null && !request.getCaption().trim().isEmpty()) {
                post.setCaption(request.getCaption().trim());
            }

            // Update images if provided
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                log.info("🔄 Updating post images...");

                // Clear existing images
                post.getImages().clear();

                // Upload new images
                List<PostImage> newImages = request.getImages().stream()
                        .map(file -> {
                            try {
                                String url = awsS3Service.saveImageToS3(file);
                                log.info("⬆ Updated image uploaded: {}", url);
                                return PostImage.builder()
                                        .imageUrl(url)
                                        .post(post)
                                        .build();
                            } catch (Exception e) {
                                log.error("❌ Failed to upload image: {}", e.getMessage());
                                throw new RuntimeException("Failed to upload image: " + file.getOriginalFilename());
                            }
                        })
                        .collect(Collectors.toList());

                post.setImages(newImages);
            }

            Post updatedPost = postRepo.save(post);
            PostDto postDto = mapper.mapPostToDto(updatedPost);

            log.info("✅ Post updated successfully: {}", postId);
            return PostResponse.builder()
                    .status(200)
                    .message("Post updated successfully")
                    .post(postDto)
                    .build();

        } catch (NotFoundException e) {
            log.error("Post not found for update: {}", e.getMessage());
            return PostResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();
        } catch (UnauthorizedException e) {
            log.error("Unauthorized post update attempt: {}", e.getMessage());
            return PostResponse.builder()
                    .status(403)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("❌ Error updating post {}: {}", postId, e.getMessage(), e);
            return PostResponse.builder()
                    .status(500)
                    .message("Failed to update post: " + e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional
    public PostResponse deletePost(Long userId, Long postId) {
        try {
            log.warn("🗑 Deleting post with ID: {} by user: {}", postId, userId);

            // Find post
            Post post = postRepo.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found"));

            // Check ownership
            if (!post.getOwner().getId().equals(userId)) {
                throw new UnauthorizedException("You can only delete your own posts");
            }

            postRepo.delete(post);

            log.info("✅ Post deleted successfully: {}", postId);
            return PostResponse.builder()
                    .status(200)
                    .message("Post deleted successfully")
                    .build();

        } catch (NotFoundException e) {
            log.error("Post not found for deletion: {}", e.getMessage());
            return PostResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();
        } catch (UnauthorizedException e) {
            log.error("Unauthorized post deletion attempt: {}", e.getMessage());
            return PostResponse.builder()
                    .status(403)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("❌ Error deleting post {}: {}", postId, e.getMessage(), e);
            return PostResponse.builder()
                    .status(500)
                    .message("Failed to delete post: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PostResponse getPostById(Long postId) {
        try {
            Post post = postRepo.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found"));

            PostDto postDto = mapper.mapPostToDto(post);

            return PostResponse.builder()
                    .status(200)
                    .post(postDto)
                    .build();

        } catch (NotFoundException e) {
            return PostResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("❌ Error retrieving post {}: {}", postId, e.getMessage(), e);
            return PostResponse.builder()
                    .status(500)
                    .message("Failed to retrieve post: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PostResponse getMyPosts(Long userId, Long currentUserId) {
        try {
            List<Post> posts = postRepo.findByOwnerIdOrderByCreatedAtDesc(userId);
            List<PostDto> postDtos = posts.stream()
                    .map(p -> mapper.mapPostToDto(p, currentUserId))
                    .collect(Collectors.toList());

            return PostResponse.builder()
                    .status(200)
                    .postList(postDtos)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error retrieving posts for user {}: {}", userId, e.getMessage(), e);
            return PostResponse.builder()
                    .status(500)
                    .message("Failed to retrieve posts: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PostResponse getAllPosts(Long currentUserId) {
        try {
            List<Post> posts = postRepo.findAllByOrderByCreatedAtDesc();
            List<PostDto> postDtos = posts.stream()
                    .map(mapper::mapPostToDto)
                    .collect(Collectors.toList());

            return PostResponse.builder()
                    .status(200)
                    .postList(postDtos)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error retrieving all posts: {}", e.getMessage(), e);
            return PostResponse.builder()
                    .status(500)
                    .message("Failed to retrieve posts: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public PostResponse getUserPosts(Long userId, Long currentUserId) {
        try {
            // Verify user exists
            if (!userRepo.existsById(userId)) {
                return PostResponse.builder()
                        .status(404)
                        .message("User not found")
                        .build();
            }

            List<Post> posts = postRepo.findByOwnerIdOrderByCreatedAtDesc(userId);
            List<PostDto> postDtos = posts.stream()
                    .map(p -> mapper.mapPostToDto(p, currentUserId))
                    .collect(Collectors.toList());

            return PostResponse.builder()
                    .status(200)
                    .postList(postDtos)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error retrieving posts for user {}: {}", userId, e.getMessage(), e);
            return PostResponse.builder()
                    .status(500)
                    .message("Failed to retrieve user posts: " + e.getMessage())
                    .build();
        }
    }
}