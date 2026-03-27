package com.cuttypaws.service.impl;

import com.cuttypaws.dto.ServiceReviewDto;
import com.cuttypaws.dto.ServiceReviewRequestDto;
import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.entity.ServiceReview;
import com.cuttypaws.entity.User;
import com.cuttypaws.enums.ServiceStatus;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.repository.ServiceProfileRepo;
import com.cuttypaws.repository.ServiceReviewRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.security.InputSanitizer;
import com.cuttypaws.service.interf.ServiceReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceReviewServiceImpl implements ServiceReviewService {

    private final ServiceProfileRepo serviceProfileRepo;
    private final ServiceReviewRepo serviceReviewRepo;
    private final UserRepo userRepo;
    private final InputSanitizer inputSanitizer;

    @Override
    @Transactional
    public UserResponse createOrUpdateReview(UUID serviceUserId, ServiceReviewRequestDto request) {
        User currentUser = getCurrentUser();
        ServiceProfile serviceProfile = getActiveServiceProfileByUserId(serviceUserId);

        validateReviewPermission(serviceProfile, currentUser);

        ServiceReview review = serviceReviewRepo
                .findByServiceProfileIdAndReviewerId(serviceProfile.getId(), currentUser.getId())
                .orElseGet(() -> ServiceReview.builder()
                        .serviceProfile(serviceProfile)
                        .reviewer(currentUser)
                        .build());

        // optional cooldown for repeated edits
        if (review.getId() != null &&
                review.getUpdatedAt() != null &&
                review.getUpdatedAt().plusMinutes(5).isAfter(LocalDateTime.now())) {
            throw new RuntimeException("You can only edit your review again after 5 minutes");
        }

        String cleanComment = sanitizeAndValidateComment(request.getComment());

        review.setRating(request.getRating());
        review.setComment(cleanComment);

        ServiceReview savedReview = serviceReviewRepo.save(review);

        updateServiceProfileRatingSummary(serviceProfile);

        return UserResponse.builder()
                .status(200)
                .message("Review submitted successfully")
                .serviceReview(mapToDto(savedReview))
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getServiceReviews(UUID serviceUserId) {
        ServiceProfile serviceProfile = getActiveServiceProfileByUserId(serviceUserId);

        List<ServiceReviewDto> reviewDtos = serviceReviewRepo
                .findByServiceProfileIdOrderByCreatedAtDesc(serviceProfile.getId())
                .stream()
                .map(this::mapToDto)
                .toList();

        return UserResponse.builder()
                .status(200)
                .message("Service reviews retrieved successfully")
                .serviceReviews(reviewDtos)
                .timeStamp(LocalDateTime.now())
                .build();
    }

    private ServiceProfile getActiveServiceProfileByUserId(UUID serviceUserId) {
        ServiceProfile serviceProfile = serviceProfileRepo.findByUserId(serviceUserId)
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        if (serviceProfile.getStatus() != ServiceStatus.ACTIVE) {
            throw new RuntimeException("Service profile not available for review");
        }

        return serviceProfile;
    }

    private void validateReviewPermission(ServiceProfile serviceProfile, User currentUser) {
        if (serviceProfile.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You cannot review your own service");
        }
    }

    private String sanitizeAndValidateComment(String comment) {
        if (comment == null) {
            return null;
        }

        String trimmed = comment.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (inputSanitizer.isMalicious(trimmed)) {
            throw new RuntimeException("Review comment contains invalid content");
        }

        String sanitized = inputSanitizer.sanitize(trimmed);

        String lower = sanitized.toLowerCase();
        if (lower.contains("http://") || lower.contains("https://") || lower.contains("www.")) {
            throw new RuntimeException("Links are not allowed in reviews");
        }

        return sanitized;
    }

    private void updateServiceProfileRatingSummary(ServiceProfile serviceProfile) {
        Double average = serviceReviewRepo.calculateAverageRating(serviceProfile.getId());
        Long count = serviceReviewRepo.countByServiceProfileId(serviceProfile.getId());

        serviceProfile.setAverageRating(average != null ? average : 0.0);
        serviceProfile.setReviewCount(count != null ? count : 0L);

        serviceProfileRepo.save(serviceProfile);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        return userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    private ServiceReviewDto mapToDto(ServiceReview review) {
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