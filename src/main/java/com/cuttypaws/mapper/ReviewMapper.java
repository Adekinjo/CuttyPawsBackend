package com.cuttypaws.mapper;

import com.cuttypaws.dto.ReviewDto;
import com.cuttypaws.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewDto mapReviewToDto(Review review) {
        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setId(review.getId());
        reviewDto.setRating(review.getRating());
        reviewDto.setComment(review.getComment());
        reviewDto.setTimestamp(review.getTimestamp());

        // Map product ID
        if (review.getProduct() != null) {
            reviewDto.setProductId(review.getProduct().getId());
        }

        // Map user ID and username
        if (review.getUser() != null) {
            reviewDto.setUserId(review.getUser().getId());
            reviewDto.setUserName(review.getUser().getName());
        }

        return reviewDto;
    }

}
