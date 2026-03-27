package com.cuttypaws.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ServiceReviewDto {
    private UUID id;
    private UUID reviewerId;
    private String reviewerName;
    private String reviewerProfileImageUrl;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}