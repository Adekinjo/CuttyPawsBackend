package com.cuttypaws.service.impl;

import com.cuttypaws.dto.ReviewDto;
import com.cuttypaws.entity.*;
import com.cuttypaws.mapper.ReviewMapper;
import com.cuttypaws.repository.*;
import com.cuttypaws.security.InputSanitizer;
import com.cuttypaws.service.interf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepo reviewRepository;
    private final ReviewMapper reviewMapper;
    private final ProductRepo productRepository;
    private final UserRepo userRepository;
    private final InputSanitizer inputSanitizer;

    // ============ REVIEW CREATION ============

    @Override
    public ReviewDto addReview(ReviewDto reviewDto) {
        log.info("Adding review for product ID: {} by user ID: {}",
                reviewDto.getProductId(), reviewDto.getUserId());

        try {
            // 1. Input Validation & Sanitization
            validateAndSanitizeReviewInput(reviewDto);

            // 2. Business Logic Validation
            Product product = validateProductExists(reviewDto.getProductId());
            User user = validateUserExists(reviewDto.getUserId());
            validateUserHasNotReviewed(reviewDto.getProductId(), reviewDto.getUserId());

            // 3. Review Creation
            Review review = createReviewEntity(reviewDto, product, user);
            Review savedReview = reviewRepository.save(review);

            // 4. Response Preparation
            ReviewDto responseDto = reviewMapper.mapReviewToDto(savedReview);
            responseDto.setUserName(user.getName());

            log.info("Review added successfully with ID: {}", savedReview.getId());
            return responseDto;

        } catch (IllegalArgumentException e) {
            log.warn("Validation error in review: {}", e.getMessage());
            throw new RuntimeException("Validation error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error adding review: {}", e.getMessage());
            throw new RuntimeException("Failed to add review: " + e.getMessage());
        }
    }

    // ============ REVIEW RETRIEVAL METHODS ============

    @Override
    public List<ReviewDto> getAllReviews() {
        log.info("Retrieving all reviews");
        List<Review> reviews = reviewRepository.findAll();
        return reviews.stream()
                .map(reviewMapper::mapReviewToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewDto> getReviewsByProductId(Long productId) {
        log.info("Retrieving reviews for product ID: {}", productId);
        List<Review> reviews = reviewRepository.findByProductId(productId);
        return reviews.stream()
                .map(reviewMapper::mapReviewToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewDto> getReviewsByUserId(Long userId) {
        log.info("Retrieving reviews by user ID: {}", userId);
        List<Review> reviews = reviewRepository.findByUserId(userId);
        return reviews.stream()
                .map(reviewMapper::mapReviewToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewDto> getReviewsByProductIdAndUserId(Long productId, Long userId) {
        log.info("Retrieving reviews for product ID: {} and user ID: {}", productId, userId);
        List<Review> reviews = reviewRepository.findByProductIdAndUserId(productId, userId);
        return reviews.stream()
                .map(reviewMapper::mapReviewToDto)
                .collect(Collectors.toList());
    }

    // ============ REVIEW MANAGEMENT ============

    @Override
    public void deleteReview(Long reviewId) {
        log.info("Deleting review with ID: {}", reviewId);

        if (!reviewRepository.existsById(reviewId)) {
            throw new RuntimeException("Review not found with ID: " + reviewId);
        }

        reviewRepository.deleteById(reviewId);
        log.info("Review deleted successfully with ID: {}", reviewId);
    }

    // ============ PRIVATE HELPER METHODS ============

    private void validateAndSanitizeReviewInput(ReviewDto reviewDto) {
        // Validate required fields
        if (reviewDto.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required");
        }
        if (reviewDto.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (reviewDto.getRating() == null || reviewDto.getRating() < 1 || reviewDto.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Sanitize comment
        if (reviewDto.getComment() != null) {
            String sanitizedComment = inputSanitizer.sanitize(reviewDto.getComment());

            // Check for malicious content
            if (inputSanitizer.isMalicious(reviewDto.getComment())) {
                throw new RuntimeException("Review contains invalid content");
            }

            // Validate comment length
            if (sanitizedComment.length() > 1000) {
                throw new RuntimeException("Review comment too long. Maximum 1000 characters allowed.");
            }

            reviewDto.setComment(sanitizedComment);
        }
    }

    private Product validateProductExists(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));
    }

    private User validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
    }

    private void validateUserHasNotReviewed(Long productId, Long userId) {
        boolean alreadyReviewed = reviewRepository.existsByProductIdAndUserId(productId, userId);
        if (alreadyReviewed) {
            throw new RuntimeException("You have already reviewed this product");
        }
    }

    private Review createReviewEntity(ReviewDto reviewDto, Product product, User user) {
        Review review = new Review();
        review.setRating(reviewDto.getRating());
        review.setComment(reviewDto.getComment());
        review.setProduct(product);
        review.setUser(user);
        review.setTimestamp(LocalDateTime.now());
        return review;
    }

}