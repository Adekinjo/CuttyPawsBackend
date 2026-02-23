package com.cuttypaws.controller;

import com.cuttypaws.dto.ReviewDto;
import com.cuttypaws.service.interf.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDto> addReview(@RequestBody ReviewDto reviewDto) {
        ReviewDto savedReview = reviewService.addReview(reviewDto);
        return ResponseEntity.ok(savedReview);
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<List<ReviewDto>> getAllReviews() {
        List<ReviewDto> reviews = reviewService.getAllReviews();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByProductId(@PathVariable Long productId) {
        List<ReviewDto> reviews = reviewService.getReviewsByProductId(productId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByUserId(@PathVariable UUID userId) {
        List<ReviewDto> reviews = reviewService.getReviewsByUserId(userId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/product/{productId}/user/{userId}")
    public ResponseEntity<List<ReviewDto>> getReviewsByProductIdAndUserId(
            @PathVariable Long productId,
            @PathVariable UUID userId) {
        List<ReviewDto> reviews = reviewService.getReviewsByProductIdAndUserId(productId, userId);
        return ResponseEntity.ok(reviews);
    }

    @DeleteMapping("/delete/{reviewId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }
}
