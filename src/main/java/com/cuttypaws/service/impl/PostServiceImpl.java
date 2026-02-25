package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.*;
import com.cuttypaws.enums.MediaType;
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
import java.util.UUID;
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
    private final CommentRepo commentRepo;


    @Override
    @Transactional
    public PostResponse createPost(UUID userId, PostRequestDto request) {
        try {
            log.info("📌 [CREATE POST] Started by userId: {}", userId);

            // Validate input
            if (request.getCaption() == null || request.getCaption().trim().isEmpty()) {
                return PostResponse.builder()
                        .status(400)
                        .message("Caption is required")
                        .build();
            }

            if (request.getMedia() == null || request.getMedia().isEmpty()) {
                return PostResponse.builder()
                        .status(400)
                        .message("At least one media file (image/video) is required")
                        .build();
            }

            // Find user
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));

            // create post
            Post post = Post.builder()
                    .caption(request.getCaption().trim())
                    .owner(user)
                    .media(new ArrayList<>())
                    .build();

            List<PostMedia> mediaList = request.getMedia().stream()
                    .map(file -> {
                        String url = awsS3Service.uploadMedia(file);
                        //MediaType type = file.getContentType().startsWith("video/") ? MediaType.VIDEO : MediaType.IMAGE;

                        String contentType = file.getContentType();
                        MediaType type = (contentType != null && contentType.startsWith("video/"))
                                ? MediaType.VIDEO
                                : MediaType.IMAGE;

                        return PostMedia.builder()
                                .mediaUrl(url)
                                .mediaType(type)
                                .post(post)
                                .build();
                    })
                    .toList();

            post.setMedia(mediaList);
            Post savedPost = postRepo.save(post);
            PostDto postDto = mapper.mapPostToDto(savedPost, userId);



            List<UUID> followerIds = followRepo.findFollowerIds(userId);
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
    public PostResponse updatePost(UUID userId, Long postId, PostRequestDto request) {
        try {
            log.info("✏ [UPDATE POST] postId={} by userId={}", postId, userId);

            Post post = postRepo.findById(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found"));

            // Ownership check
            if (post.getOwner() == null || !userId.equals(post.getOwner().getId())) {
                throw new UnauthorizedException("You can only update your own posts");
            }

            // Update caption (optional)
            if (request.getCaption() != null && !request.getCaption().trim().isEmpty()) {
                post.setCaption(request.getCaption().trim());
            }

            // Ensure list is initialized
            if (post.getMedia() == null) {
                post.setMedia(new ArrayList<>());
            }

            // 1) Delete requested media (optional)
            if (request.getMediaToDelete() != null && !request.getMediaToDelete().isEmpty()) {
                log.info("🗑 Removing {} media items from post {}", request.getMediaToDelete().size(), postId);

                // Remove from collection (orphanRemoval=true will delete rows)
                post.getMedia().removeIf(m -> m.getId() != null && request.getMediaToDelete().contains(m.getId()));
            }

            // 2) Add new media uploads (optional)
            if (request.getMedia() != null && !request.getMedia().isEmpty()) {
                log.info("📤 Uploading {} new media files for post {}", request.getMedia().size(), postId);

                for (var file : request.getMedia()) {
                    if (file == null || file.isEmpty()) continue;

                    String url = awsS3Service.uploadMedia(file);

                    String contentType = file.getContentType();
                    MediaType type = (contentType != null && contentType.startsWith("video/"))
                            ? MediaType.VIDEO
                            : MediaType.IMAGE;

                    PostMedia pm = PostMedia.builder()
                            .mediaUrl(url)
                            .mediaType(type)
                            .post(post)
                            .build();

                    post.getMedia().add(pm);
                }
            }

            // Optional safety: prevent empty post (no caption AND no media)
            boolean hasCaption = post.getCaption() != null && !post.getCaption().trim().isEmpty();
            boolean hasMedia = post.getMedia() != null && !post.getMedia().isEmpty();
            if (!hasCaption && !hasMedia) {
                return PostResponse.builder()
                        .status(400)
                        .message("Post cannot be empty. Provide caption or at least one media file.")
                        .build();
            }

            Post updatedPost = postRepo.save(post);

            // use userId as current user for isLikedByCurrentUser
            PostDto postDto = mapper.mapPostToDto(updatedPost, userId);

            log.info("✅ Post updated successfully: postId={}", postId);

            return PostResponse.builder()
                    .status(200)
                    .message("Post updated successfully")
                    .post(postDto)
                    .build();

        } catch (NotFoundException e) {
            log.error("❌ Post not found: {}", e.getMessage());
            return PostResponse.builder()
                    .status(404)
                    .message(e.getMessage())
                    .build();

        } catch (UnauthorizedException e) {
            log.error("❌ Unauthorized update: {}", e.getMessage());
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
    public PostResponse deletePost(UUID userId, Long postId) {
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
            Post post = postRepo.findByIdWithLikesAndMedia(postId)
                    .orElseThrow(() -> new NotFoundException("Post not found"));

            PostDto postDto = mapper.mapPostToDto(post,null);

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
    @Transactional(readOnly = true)
    public PostResponse getMyPosts(UUID userId, UUID currentUserId) {
        try {
            List<Post> posts = postRepo.findByOwnerIdWithLikesAndMedia(userId);
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
    public PostResponse getAllPosts() {
        try {
            List<Post> posts = postRepo.findAllWithMedia();

            List<PostDto> postDtos = posts.stream()
                    .map(p -> mapper.mapPostToDto(p, null))
                    .toList();

            return PostResponse.builder()
                    .status(200)
                    .postList(postDtos)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error retrieving all posts: {}", e.getMessage(), e);
            return PostResponse.builder()
                    .status(500)
                    .message("Failed to retrieve posts")
                    .build();
        }
    }


    @Override
    @Transactional(readOnly = true)
    public PostResponse getUserPosts(UUID userId, UUID currentUserId) {
        try {
            // Verify user exists
            if (!userRepo.existsById(userId)) {
                return PostResponse.builder()
                        .status(404)
                        .message("User not found")
                        .build();
            }

            List<Post> posts = postRepo.findByOwnerIdWithLikesAndMedia(userId);
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