package com.cuttypaws.service.interf;

import com.cuttypaws.dto.*;

import java.util.List;

public interface ReviewService {

    // Add a new review
    ReviewDto addReview(ReviewDto reviewDto);


    List<ReviewDto> getAllReviews();
    // Get all reviews for a product (including user name)
    List<ReviewDto> getReviewsByProductId(Long productId);

    // Get all reviews by a user (including user name)
    List<ReviewDto> getReviewsByUserId(Long userId);

    // Get all reviews for a product by a specific user (including user name)
    List<ReviewDto> getReviewsByProductIdAndUserId(Long productId, Long userId);

    // Delete a review by ID
    void deleteReview(Long reviewId);
}