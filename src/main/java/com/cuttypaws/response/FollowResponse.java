package com.cuttypaws.response;

import com.cuttypaws.dto.FollowDto;
import com.cuttypaws.dto.FollowStatsDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FollowResponse {


    private int status;
    private String message;

    private int totalPage;
    private long totalElement;
    private LocalDateTime timeStamp;
    private FollowStatsDto followStats;
    private List<FollowDto> followersList;
    private List<FollowDto> followingList;
    private Boolean isFollowing;
    private Boolean isFollowedBy;

}
