package com.cuttypaws.mapper;

import com.cuttypaws.dto.PostDto;
import com.cuttypaws.entity.Post;
import com.cuttypaws.entity.PostImage;
import com.cuttypaws.repository.PostLikeRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostMapper {

    private final PostLikeRepo postLikeRepo;

    public PostDto mapPostToDto(Post post) {
        return mapPostToDto(post, null);
    }

    public PostDto mapPostToDto(Post post, Long currentUserId) {
        if (post == null) {
            log.warn("Attempted to map null post to DTO");
            return null;
        }

        try {
            PostDto.PostDtoBuilder builder = PostDto.builder()
                    .id(post.getId())
                    .caption(post.getCaption())
                    .ownerId(post.getOwner() != null ? post.getOwner().getId() : null)
                    .ownerName(post.getOwner() != null ? post.getOwner().getName() : "Unknown User")
                    .ownerProfileImage(post.getOwner() != null ? post.getOwner().getProfileImageUrl() : null)
                    .createdAt(post.getCreatedAt() != null ?
                            post.getCreatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null)
                    .updatedAt(post.getUpdatedAt() != null ?
                            post.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME) : null)
                    .imageUrls(
                            post.getImages() != null ?
                                    post.getImages().stream()
                                            .map(PostImage::getImageUrl)
                                            .collect(Collectors.toList()) :
                                    List.of()
                    )
                    .likeCount(post.getLikeCount())
                    .commentCount(post.getCommentCount());

            // FIX: Use repository instead of Post entity's cached collection
            if (currentUserId != null) {
                boolean isLiked = postLikeRepo.existsByUserIdAndPostId(currentUserId, post.getId());
                builder.isLikedByCurrentUser(isLiked);
            }

            PostDto postDto = builder.build();
            log.debug("Mapped post {} to DTO for user {}", post.getId(), currentUserId);
            return postDto;

        } catch (Exception e) {
            log.error("❌ Error mapping post {} to DTO: {}", post.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to map post to DTO: " + e.getMessage());
        }
    }


}
