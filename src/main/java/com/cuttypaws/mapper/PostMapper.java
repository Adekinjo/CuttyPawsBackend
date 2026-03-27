//package com.cuttypaws.mapper;
//
//import com.cuttypaws.dto.MediaDto;
//import com.cuttypaws.dto.PostDto;
//import com.cuttypaws.entity.Post;
//import com.cuttypaws.entity.PostMedia;
//import com.cuttypaws.enums.MediaType;
//import com.cuttypaws.repository.CommentRepo;
//import com.cuttypaws.repository.PostLikeRepo;
//import com.cuttypaws.repository.PostRepo;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PostMapper {
//
//    private final PostLikeRepo postLikeRepo;
//    private final CommentRepo commentRepo;
//
//    public PostDto mapPostToDto(Post post, UUID currentUserId) {
//
//        List<MediaDto> media = post.getMedia() == null ? List.of() :
//                post.getMedia().stream()
//                        .map(m -> MediaDto.builder()
//                                .url(m.getMediaUrl())
//                                .type(m.getMediaType().name())
//                                .thumbnailUrl(m.getThumbnailUrl())
//                                .build())
//                        .toList();
//
//        List<String> imageUrls = post.getMedia() == null ? List.of() :
//                post.getMedia().stream()
//                        .filter(m -> m.getMediaType() == MediaType.IMAGE)
//                        .map(PostMedia::getMediaUrl)
//                        .toList();
//
//        PostDto.PostDtoBuilder builder = PostDto.builder()
//                .id(post.getId())
//                .caption(post.getCaption())
//                .ownerId(post.getOwner() != null ? post.getOwner().getId() : null)
//                .ownerName(post.getOwner() != null ? post.getOwner().getName() : "Unknown")
//                .ownerProfileImage(post.getOwner() != null ? post.getOwner().getProfileImageUrl() : null)
//                .media(media)
//                .imageUrls(imageUrls)
//                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().toString() : null)
//                .updatedAt(post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : null);
//
//        builder.likeCount(postLikeRepo.countByPostId(post.getId()).intValue());
//        builder.commentCount(commentRepo.countByPostId(post.getId()).intValue());
//
//        if (currentUserId != null) {
//            builder.isLikedByCurrentUser(
//                    postLikeRepo.existsByUserIdAndPostId(currentUserId, post.getId())
//            );
//        }
//
//        return builder.build();
//    }
//
//    public PostDto mapPostToDtoFast(
//            Post post,
//            int likeCount,
//            int commentCount,
//            Boolean isLikedByCurrentUser
//    ) {
//
//        List<MediaDto> media = post.getMedia() == null ? List.of() :
//                post.getMedia().stream()
//                        .map(m -> MediaDto.builder()
//                                .url(m.getMediaUrl())
//                                .type(m.getMediaType().name())
//                                .thumbnailUrl(m.getThumbnailUrl())
//                                .build())
//                        .toList();
//
//        List<String> imageUrls = post.getMedia() == null ? List.of() :
//                post.getMedia().stream()
//                        .filter(m -> m.getMediaType() == MediaType.IMAGE)
//                        .map(PostMedia::getMediaUrl)
//                        .toList();
//
//        return PostDto.builder()
//                .id(post.getId())
//                .caption(post.getCaption())
//                .ownerId(post.getOwner() != null ? post.getOwner().getId() : null)
//                .ownerName(post.getOwner() != null ? post.getOwner().getName() : "Unknown")
//                .ownerProfileImage(post.getOwner() != null ? post.getOwner().getProfileImageUrl() : null)
//                .media(media)
//                .imageUrls(imageUrls)
//                .likeCount(likeCount)
//                .commentCount(commentCount)
//                .isLikedByCurrentUser(isLikedByCurrentUser)
//                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().toString() : null)
//                .updatedAt(post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : null)
//                .build();
//    }
//
//}




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

        List<MediaDto> media = post.getMedia() == null ? List.of() :
                post.getMedia().stream()
                        .map(this::mapMediaToDto)
                        .toList();

        List<String> imageUrls = post.getMedia() == null ? List.of() :
                post.getMedia().stream()
                        .filter(m -> m.getMediaType() == MediaType.IMAGE)
                        .map(PostMedia::getMediaUrl)
                        .toList();

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

    public PostDto mapPostToDtoFast(
            Post post,
            int likeCount,
            int commentCount,
            Boolean isLikedByCurrentUser
    ) {

        List<MediaDto> media = post.getMedia() == null ? List.of() :
                post.getMedia().stream()
                        .map(this::mapMediaToDto)
                        .toList();

        List<String> imageUrls = post.getMedia() == null ? List.of() :
                post.getMedia().stream()
                        .filter(m -> m.getMediaType() == MediaType.IMAGE)
                        .map(PostMedia::getMediaUrl)
                        .toList();

        return PostDto.builder()
                .id(post.getId())
                .caption(post.getCaption())
                .ownerId(post.getOwner() != null ? post.getOwner().getId() : null)
                .ownerName(post.getOwner() != null ? post.getOwner().getName() : "Unknown")
                .ownerProfileImage(post.getOwner() != null ? post.getOwner().getProfileImageUrl() : null)
                .media(media)
                .imageUrls(imageUrls)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .isLikedByCurrentUser(isLikedByCurrentUser)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().toString() : null)
                .updatedAt(post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : null)
                .build();
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