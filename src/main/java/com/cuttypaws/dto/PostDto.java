package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {

    private Long id;
    private String caption;

    private Long ownerId;
    private String ownerName;
    private String ownerProfileImage;

    private List<String> imageUrls;

    private Integer likeCount;
    private Boolean isLikedByCurrentUser;
    private Integer commentCount;

    private String createdAt;
    private String updatedAt;
}

