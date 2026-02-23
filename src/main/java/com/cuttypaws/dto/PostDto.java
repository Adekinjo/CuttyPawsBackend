package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {

    private Long id;
    private String caption;
    private List<MediaDto> media;

    private UUID ownerId;
    private String ownerName;
    private String ownerProfileImage;

    private List<String> imageUrls;

    private Integer likeCount;
    private Boolean isLikedByCurrentUser;
    private Integer commentCount;

    private String createdAt;
    private String updatedAt;
}