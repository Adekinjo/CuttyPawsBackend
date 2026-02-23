package com.cuttypaws.mapper;

import com.cuttypaws.dto.MediaDto;
import com.cuttypaws.dto.PostDto;
import com.cuttypaws.entity.Post;
import com.cuttypaws.entity.PostMedia;
import com.cuttypaws.enums.MediaType;
import com.cuttypaws.repository.CommentRepo;
import com.cuttypaws.repository.PostLikeRepo;
import com.cuttypaws.repository.PostRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostMapper {

    private final PostLikeRepo postLikeRepo;
    private final CommentRepo commentRepo;

    public PostDto mapPostToDto(Post post, UUID currentUserId) {

        List<MediaDto> media = post.getMedia() == null ? List.of() :
                post.getMedia().stream()
                        .map(m -> MediaDto.builder()
                                .url(m.getMediaUrl())
                                .type(m.getMediaType().name())
                                .thumbnailUrl(m.getThumbnailUrl())
                                .build())
                        .toList();

        // 🔍 DEBUG MEDIA FROM ENTITY
        if (post.getMedia() == null || post.getMedia().isEmpty()) {
            log.warn("⚠️ Post {} has NO media in entity", post.getId());
        } else {
            post.getMedia().forEach(m ->
                    log.info("📷 Post {} MEDIA from DB → {}", post.getId(), m.getMediaUrl())
            );
        }

        List<String> imageUrls = post.getMedia() == null ? List.of() :
                post.getMedia().stream()
                        .filter(m -> m.getMediaType() == MediaType.IMAGE)
                        .map(PostMedia::getMediaUrl)
                        .toList();

        // 🔍 DEBUG IMAGE URLS SENT TO FRONTEND
        imageUrls.forEach(url ->
                log.info("🌐 Post {} IMAGE URL sent to frontend → {}", post.getId(), url)
        );

        PostDto.PostDtoBuilder builder = PostDto.builder()
                .id(post.getId())
                .caption(post.getCaption())
                .ownerId(post.getOwner() != null ? post.getOwner().getId() : null)
                .ownerName(post.getOwner() != null ? post.getOwner().getName() : "Unknown")
                .ownerProfileImage(post.getOwner() != null ? post.getOwner().getProfileImageUrl() : null)
                .media(media)
                .imageUrls(imageUrls)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().toString() : null)
                .updatedAt(post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : null);

        builder.likeCount(postLikeRepo.countByPostId(post.getId()).intValue());
        builder.commentCount(commentRepo.countByPostId(post.getId()).intValue());

        if (currentUserId != null) {
            builder.isLikedByCurrentUser(
                    postLikeRepo.existsByUserIdAndPostId(currentUserId, post.getId())
            );
        }

        return builder.build();
    }


}