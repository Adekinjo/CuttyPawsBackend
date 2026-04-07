package com.cuttypaws.mapper;

import com.cuttypaws.dto.MediaDto;
import com.cuttypaws.dto.PostDto;
import com.cuttypaws.entity.Post;
import com.cuttypaws.entity.PostMedia;
import com.cuttypaws.enums.MediaType;
import com.cuttypaws.repository.CommentRepo;
import com.cuttypaws.repository.PostLikeRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostMapper {

    private final PostLikeRepo postLikeRepo;
    private final CommentRepo commentRepo;

    public PostDto mapPostToDto(Post post, UUID currentUserId) {

        List<MediaDto> media = buildMedia(post);
        List<String> imageUrls = buildImageUrls(post);

        int likeCount = postLikeRepo.countByPostId(post.getId()).intValue();
        int commentCount = commentRepo.countByPostId(post.getId()).intValue();

        Boolean likedByUser = null;

        if (currentUserId != null) {
            likedByUser = postLikeRepo.existsByUserIdAndPostId(currentUserId, post.getId());
        }

        return buildPostDto(post, media, imageUrls, likeCount, commentCount, likedByUser);
    }

    public PostDto mapPostToDtoFast(
            Post post,
            int likeCount,
            int commentCount,
            Boolean isLikedByCurrentUser
    ) {

        List<MediaDto> media = buildMedia(post);
        List<String> imageUrls = buildImageUrls(post);

        return buildPostDto(post, media, imageUrls, likeCount, commentCount, isLikedByCurrentUser);
    }

    private PostDto buildPostDto(
            Post post,
            List<MediaDto> media,
            List<String> imageUrls,
            int likeCount,
            int commentCount,
            Boolean likedByUser
    ) {

        return PostDto.builder()
                .id(post.getId())
                .caption(post.getCaption())
                .ownerId(post.getOwner() != null ? post.getOwner().getId() : null)
                .ownerName(post.getOwner() != null ? post.getOwner().getName() : "Unknown")
                .ownerProfileImage(post.getOwner() != null ? post.getOwner().getProfileImageUrl() : null)
                .ownerRole(
                        post.getOwner() != null && post.getOwner().getUserRole() != null
                                ? post.getOwner().getUserRole().name()
                                : null
                )
                .media(media)
                .imageUrls(imageUrls)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .isLikedByCurrentUser(likedByUser)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().toString() : null)
                .updatedAt(post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : null)
                .build();
    }

    private List<MediaDto> buildMedia(Post post) {

        if (post.getMedia() == null || post.getMedia().isEmpty()) {
            return List.of();
        }

        return post.getMedia()
                .stream()
                .map(this::mapMediaToDto)
                .toList();
    }

    private List<String> buildImageUrls(Post post) {

        if (post.getMedia() == null || post.getMedia().isEmpty()) {
            return List.of();
        }

        return post.getMedia()
                .stream()
                .filter(m -> m.getMediaType() == MediaType.IMAGE)
                .map(PostMedia::getMediaUrl)
                .toList();
    }

    private MediaDto mapMediaToDto(PostMedia media) {

        return MediaDto.builder()
                .url(media.getMediaUrl())
                .type(media.getMediaType().name())
                .thumbnailUrl(media.getThumbnailUrl())
                .streamUrl(media.getStreamUrl())
                .durationSeconds(media.getDurationSeconds())
                .processed(media.getProcessed())
                .build();
    }
}