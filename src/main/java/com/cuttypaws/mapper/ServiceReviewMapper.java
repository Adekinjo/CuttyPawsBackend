package com.cuttypaws.mapper;

import com.cuttypaws.dto.ServiceReviewDto;
import com.cuttypaws.entity.ServiceReview;
import org.springframework.stereotype.Component;

@Component
public class ServiceReviewMapper {

    public ServiceReviewDto toDto(ServiceReview review) {
        if (review == null) {
            return null;
        }

        return ServiceReviewDto.builder()
                .id(review.getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getName())
                .reviewerProfileImageUrl(review.getReviewer().getProfileImageUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}