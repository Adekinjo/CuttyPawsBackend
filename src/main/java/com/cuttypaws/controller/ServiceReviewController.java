package com.cuttypaws.controller;

import com.cuttypaws.dto.ServiceReviewRequestDto;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.interf.ServiceReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/service-reviews")
@RequiredArgsConstructor
public class ServiceReviewController {

    private final ServiceReviewService serviceReviewService;

    @PostMapping("/{serviceUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> createOrUpdateReview(
            @PathVariable UUID serviceUserId,
            @Valid @RequestBody ServiceReviewRequestDto request
    ) {
        UserResponse response = serviceReviewService.createOrUpdateReview(serviceUserId, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{serviceUserId}")
    public ResponseEntity<UserResponse> getServiceReviews(@PathVariable UUID serviceUserId) {
        UserResponse response = serviceReviewService.getServiceReviews(serviceUserId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}