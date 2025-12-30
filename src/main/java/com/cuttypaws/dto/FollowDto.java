package com.cuttypaws.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FollowDto {
    private Long id;
    private UserBasicDto follower;
    private UserBasicDto following;
    private LocalDateTime createdAt;
    private Boolean isMuted;
}

