package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {
    private UUID userId;
    private Long postCount;
    private Long totalLikes;
    private Long totalComments;
    private Long followersCount;
    private Long followingCount;
    private Boolean isFollowing;
    private Boolean isFollowedBy;
}

