package com.cuttypaws.service.interf;

import com.cuttypaws.dto.ServiceReviewRequestDto;
import com.cuttypaws.response.UserResponse;

import java.util.UUID;

public interface ServiceReviewService {
    UserResponse createOrUpdateReview(UUID serviceUserId, ServiceReviewRequestDto request);
    UserResponse getServiceReviews(UUID serviceUserId);
}