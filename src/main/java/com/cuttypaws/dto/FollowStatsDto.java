package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowStatsDto {
    private Long userId;
    private Long followersCount;
    private Long followingCount;
    private Boolean isFollowing;
    private Boolean isFollowedBy;
}

