package com.cuttypaws.service.interf;

import com.cuttypaws.dto.ServiceProfileRequestDto;
import com.cuttypaws.dto.ServiceProviderRegistrationRequest;
import com.cuttypaws.response.UserResponse;

import java.util.UUID;

public interface ServiceProviderService {
    UserResponse registerServiceProvider(ServiceProviderRegistrationRequest request);
    UserResponse updateMyServiceProfile(ServiceProfileRequestDto request);
    UserResponse getMyServiceProfile();
    UserResponse getMyServiceDashboard();
    UserResponse getPublicServiceProfile(UUID userId);
    UserResponse getPendingServiceRegistrations();
    UserResponse approveServiceRegistration(UUID userId);
    UserResponse rejectServiceRegistration(UUID userId, String reason);
    UserResponse uploadMyServiceMedia(java.util.List<org.springframework.web.multipart.MultipartFile> files);
    UserResponse getMyServiceMedia();
    UserResponse deleteMyServiceMedia(java.util.UUID mediaId);
    UserResponse setMyServiceMediaCover(java.util.UUID mediaId);
}