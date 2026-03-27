package com.cuttypaws.service.impl;

import com.cuttypaws.dto.*;
import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.entity.ServiceMedia;
import com.cuttypaws.entity.ServiceProfile;
import com.cuttypaws.entity.User;
import com.cuttypaws.enums.ServiceStatus;
import com.cuttypaws.enums.UserRole;
import com.cuttypaws.exception.NotFoundException;
import com.cuttypaws.exception.UserAlreadyExistsException;
import com.cuttypaws.mapper.ServiceProviderMapper;
import com.cuttypaws.mapper.UserMapper;
import com.cuttypaws.mapper.ServiceAdSubscriptionMapper;
import com.cuttypaws.repository.ServiceAdSubscriptionRepo;
import com.cuttypaws.repository.ServiceMediaRepo;
import com.cuttypaws.repository.ServiceProfileRepo;
import com.cuttypaws.repository.UserRepo;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.security.InputSanitizer;
import com.cuttypaws.service.AwsS3Service;
import com.cuttypaws.service.EmailService;
import com.cuttypaws.service.interf.ServiceProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceProviderServiceImpl implements ServiceProviderService {

    private final UserRepo userRepo;
    private final ServiceProfileRepo serviceProfileRepo;
    private final ServiceAdSubscriptionRepo serviceAdSubscriptionRepo;
    private final PasswordEncoder passwordEncoder;
    private final InputSanitizer inputSanitizer;
    private final ServiceProviderMapper serviceProviderMapper;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final ServiceAdSubscriptionMapper serviceAdSubscriptionMapper;
    private final ServiceMediaRepo serviceMediaRepo;
    private final AwsS3Service awsS3Service;

    private static final String STRONG_PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$";

    @Value("${app.service-review.admin-email:cuttypawsinfo@gmail.com}")
    private String adminReviewEmail;

    @Override
    @Transactional
    public UserResponse registerServiceProvider(ServiceProviderRegistrationRequest request) {
        UserDto userRequest = request.getUser();
        ServiceProfileRequestDto serviceRequest = request.getServiceProfile();

        validateRegistrationInput(userRequest);
        sanitizeServiceProfileRequest(serviceRequest);
        validateServiceProfileRequest(serviceRequest);

        if (userRepo.findByEmail(userRequest.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        if (userRepo.findByPhoneNumber(userRequest.getPhoneNumber()).isPresent()) {
            throw new UserAlreadyExistsException("Phone number is already registered");
        }

        if (!Pattern.matches(STRONG_PASSWORD_REGEX, userRequest.getPassword())) {
            throw new RuntimeException(
                    "Password must be at least 8 characters with uppercase, lowercase, number and special character"
            );
        }

        User user = User.builder()
                .name(userRequest.getName())
                .email(userRequest.getEmail())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .phoneNumber(userRequest.getPhoneNumber())
                .companyName(userRequest.getCompanyName())
                .businessRegistrationNumber(userRequest.getBusinessRegistrationNumber())
                .userRole(UserRole.ROLE_SERVICE_PROVIDER)
                .isServiceProvider(true)
                .petsCount(0)
                .build();

        User savedUser = userRepo.save(user);

        ServiceProfile serviceProfile = ServiceProfile.builder()
                .user(savedUser)
                .serviceType(serviceRequest.getServiceType())
                .businessName(serviceRequest.getBusinessName())
                .tagline(serviceRequest.getTagline())
                .description(serviceRequest.getDescription())
                .city(serviceRequest.getCity())
                .state(serviceRequest.getState())
                .country(serviceRequest.getCountry())
                .zipcode(serviceRequest.getZipcode())
                .serviceArea(serviceRequest.getServiceArea())
                .addressLine(serviceRequest.getAddressLine())
                .latitude(serviceRequest.getLatitude())
                .longitude(serviceRequest.getLongitude())
                .priceFrom(serviceRequest.getPriceFrom())
                .priceTo(serviceRequest.getPriceTo())
                .pricingNote(serviceRequest.getPricingNote())
                .yearsOfExperience(serviceRequest.getYearsOfExperience())
                .licenseNumber(serviceRequest.getLicenseNumber())
                .websiteUrl(serviceRequest.getWebsiteUrl())
                .whatsappNumber(serviceRequest.getWhatsappNumber())
                .acceptsHomeVisits(Boolean.TRUE.equals(serviceRequest.getAcceptsHomeVisits()))
                .offersEmergencyService(Boolean.TRUE.equals(serviceRequest.getOffersEmergencyService()))
                .status(ServiceStatus.PENDING)
                .build();

        ServiceProfile savedProfile = serviceProfileRepo.save(serviceProfile);

        sendServiceReviewPendingEmail(savedUser);
        notifyAdminOfPendingRegistration(savedUser, savedProfile);

        return UserResponse.builder()
                .status(201)
                .message("Service provider registration submitted successfully and is under review")
                .user(userMapper.mapUserToDtoBasic(savedUser))
                .serviceProfile(serviceProviderMapper.toDto(savedProfile))
                .timeStamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateMyServiceProfile(ServiceProfileRequestDto request) {
        User currentUser = getCurrentUser();

        if (!Boolean.TRUE.equals(currentUser.getIsServiceProvider())) {
            throw new RuntimeException("User is not a service provider");
        }

        sanitizeServiceProfileRequest(request);
        validateServiceProfileRequest(request);

        ServiceProfile profile = serviceProfileRepo.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        if (request.getServiceType() != null) profile.setServiceType(request.getServiceType());
        if (request.getBusinessName() != null) profile.setBusinessName(request.getBusinessName());
        if (request.getTagline() != null) profile.setTagline(request.getTagline());
        if (request.getDescription() != null) profile.setDescription(request.getDescription());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getState() != null) profile.setState(request.getState());
        if (request.getCountry() != null) profile.setCountry(request.getCountry());
        if (request.getZipcode() != null) profile.setZipcode(request.getZipcode());
        if (request.getServiceArea() != null) profile.setServiceArea(request.getServiceArea());
        if (request.getAddressLine() != null) profile.setAddressLine(request.getAddressLine());
        if (request.getLatitude() != null) profile.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) profile.setLongitude(request.getLongitude());
        if (request.getPriceFrom() != null) profile.setPriceFrom(request.getPriceFrom());
        if (request.getPriceTo() != null) profile.setPriceTo(request.getPriceTo());
        if (request.getPricingNote() != null) profile.setPricingNote(request.getPricingNote());
        if (request.getYearsOfExperience() != null) profile.setYearsOfExperience(request.getYearsOfExperience());
        if (request.getLicenseNumber() != null) profile.setLicenseNumber(request.getLicenseNumber());
        if (request.getWebsiteUrl() != null) profile.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getWhatsappNumber() != null) profile.setWhatsappNumber(request.getWhatsappNumber());
        if (request.getAcceptsHomeVisits() != null) profile.setAcceptsHomeVisits(request.getAcceptsHomeVisits());
        if (request.getOffersEmergencyService() != null) profile.setOffersEmergencyService(request.getOffersEmergencyService());

        ServiceProfile updated = serviceProfileRepo.save(profile);
        List<ServiceMediaDto> media = getServiceMediaDtos(updated.getId());

        return UserResponse.builder()
                .status(200)
                .message("Service profile updated successfully")
                .serviceProfile(serviceProviderMapper.toDto(updated, media))
                .timeStamp(LocalDateTime.now())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyServiceProfile() {
        User currentUser = getCurrentUser();

        if (!Boolean.TRUE.equals(currentUser.getIsServiceProvider())) {
            throw new RuntimeException("User is not a service provider");
        }

        ServiceProfile profile = serviceProfileRepo.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        List<ServiceMediaDto> media = getServiceMediaDtos(profile.getId());

        return UserResponse.builder()
                .status(200)
                .message(resolveServiceStatusMessage(profile.getStatus()))
                .serviceProfile(serviceProviderMapper.toDto(profile, media))
                .timeStamp(LocalDateTime.now())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyServiceDashboard() {
        User currentUser = getCurrentUser();

        if (!Boolean.TRUE.equals(currentUser.getIsServiceProvider())) {
            return UserResponse.builder()
                    .status(403)
                    .message("User is not a service provider")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        ServiceProfile profile = serviceProfileRepo.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        ServiceAdSubscription activeAd = serviceAdSubscriptionRepo
                .findFirstByServiceProfileIdAndIsActiveTrueAndEndsAtAfterOrderByCreatedAtDesc(
                        profile.getId(),
                        LocalDateTime.now()
                )
                .orElse(null);

        List<ServiceMediaDto> media = getServiceMediaDtos(profile.getId());

        ServiceDashboardDto dashboardDto = ServiceDashboardDto.builder()
                .status(profile.getStatus())
                .canAccessDashboard(profile.getStatus() == ServiceStatus.ACTIVE)
                .statusMessage(resolveServiceStatusMessage(profile.getStatus()))
                .rejectionReason(profile.getRejectionReason())
                .serviceProfile(serviceProviderMapper.toDto(profile, media))
                .activeAdSubscription(serviceAdSubscriptionMapper.toDto(activeAd))
                .build();

        int statusCode = profile.getStatus() == ServiceStatus.ACTIVE ? 200 : 403;

        return UserResponse.builder()
                .status(statusCode)
                .message(resolveServiceStatusMessage(profile.getStatus()))
                .serviceDashboard(dashboardDto)
                .timeStamp(LocalDateTime.now())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponse getPublicServiceProfile(UUID userId) {
        ServiceProfile profile = serviceProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        if (profile.getStatus() != ServiceStatus.ACTIVE) {
            return UserResponse.builder()
                    .status(404)
                    .message("Service profile not available")
                    .timeStamp(LocalDateTime.now())
                    .build();
        }

        List<ServiceMediaDto> media = getServiceMediaDtos(profile.getId());
        ServiceProfileDto dto = serviceProviderMapper.toDto(profile, media);

        ServiceAdSubscription activeAd = serviceAdSubscriptionRepo
                .findFirstByServiceProfileIdAndIsActiveTrueAndEndsAtAfterOrderByCreatedAtDesc(
                        profile.getId(),
                        LocalDateTime.now()
                )
                .orElse(null);

        if (activeAd != null) {
            dto.setSponsored(true);
            dto.setSponsoredPlanType(activeAd.getPlanType().name());
            dto.setSponsoredUntil(activeAd.getEndsAt());
        }

        return UserResponse.builder()
                .status(200)
                .message("Public service profile retrieved successfully")
                .serviceProfile(dto)
                .timeStamp(LocalDateTime.now())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public UserResponse getPendingServiceRegistrations() {
        List<ServiceProfileDto> pendingProfiles = serviceProfileRepo
                .findByStatusOrderByCreatedAtAsc(ServiceStatus.PENDING)
                .stream()
                .map(profile -> serviceProviderMapper.toDto(profile, getServiceMediaDtos(profile.getId())))
                .collect(Collectors.toList());

        return UserResponse.builder()
                .status(200)
                .message("Pending service registrations retrieved successfully")
                .serviceProfiles(pendingProfiles)
                .timeStamp(LocalDateTime.now())
                .build();
    }


    @Override
    @Transactional
    public UserResponse approveServiceRegistration(UUID userId) {
        ServiceProfile profile = serviceProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        profile.setStatus(ServiceStatus.ACTIVE);
        profile.setRejectionReason(null);
        profile.setReviewedAt(LocalDateTime.now());
        profile.setApprovedAt(LocalDateTime.now());

        ServiceProfile updatedProfile = serviceProfileRepo.save(profile);
        List<ServiceMediaDto> media = getServiceMediaDtos(updatedProfile.getId());

        sendServiceApprovedEmail(profile.getUser());

        return UserResponse.builder()
                .status(200)
                .message("Service registration approved successfully")
                .serviceProfile(serviceProviderMapper.toDto(updatedProfile, media))
                .timeStamp(LocalDateTime.now())
                .build();
    }


    @Override
    @Transactional
    public UserResponse rejectServiceRegistration(UUID userId, String reason) {
        ServiceProfile profile = serviceProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Service profile not found"));

        profile.setStatus(ServiceStatus.REJECTED);
        profile.setRejectionReason(reason != null && !reason.isBlank() ? reason.trim() : "Not specified");
        profile.setReviewedAt(LocalDateTime.now());

        ServiceProfile updatedProfile = serviceProfileRepo.save(profile);
        List<ServiceMediaDto> media = getServiceMediaDtos(updatedProfile.getId());

        sendServiceRejectedEmail(profile.getUser(), profile.getRejectionReason());

        return UserResponse.builder()
                .status(200)
                .message("Service registration rejected successfully")
                .serviceProfile(serviceProviderMapper.toDto(updatedProfile, media))
                .timeStamp(LocalDateTime.now())
                .build();
    }


    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Current user not found"));
    }

    @Override
    public UserResponse uploadMyServiceMedia(List<MultipartFile> files) {

        User user = getCurrentUser();

        ServiceProfile profile = serviceProfileRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Service profile not found"));

        List<ServiceMediaDto> result = new ArrayList<>();

        int order = serviceMediaRepo
                .findByServiceProfileIdOrderByDisplayOrderAsc(profile.getId())
                .size();

        for (MultipartFile file : files) {

            String contentType = file.getContentType();

            com.cuttypaws.enums.MediaType type =
                    contentType != null && contentType.startsWith("video")
                            ? com.cuttypaws.enums.MediaType.VIDEO
                            : com.cuttypaws.enums.MediaType.IMAGE;

            String url = awsS3Service.uploadMedia(file, "service-media");

            ServiceMedia media = ServiceMedia.builder()
                    .serviceProfile(profile)
                    .mediaType(type)
                    .mediaUrl(url)
                    .displayOrder(order++)
                    .isCover(false)
                    .build();

            serviceMediaRepo.save(media);

            result.add(
                    ServiceMediaDto.builder()
                            .id(media.getId())
                            .mediaType(media.getMediaType().name())
                            .mediaUrl(media.getMediaUrl())
                            .isCover(media.getIsCover())
                            .build()
            );
        }

        return UserResponse.builder()
                .status(200)
                .message("Media uploaded")
                .serviceMediaList(result)
                .build();
    }

    @Override
    public UserResponse getMyServiceMedia() {

        User user = getCurrentUser();

        ServiceProfile profile = serviceProfileRepo.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Service profile not found"));

        List<ServiceMediaDto> list = serviceMediaRepo
                .findByServiceProfileIdOrderByDisplayOrderAsc(profile.getId())
                .stream()
                .map(m -> ServiceMediaDto.builder()
                        .id(m.getId())
                        .mediaType(m.getMediaType().name())
                        .mediaUrl(m.getMediaUrl())
                        .isCover(m.getIsCover())
                        .build())
                .toList();

        return UserResponse.builder()
                .status(200)
                .serviceMediaList(list)
                .build();
    }

    @Override
    public UserResponse deleteMyServiceMedia(UUID mediaId) {

        User user = getCurrentUser();

        ServiceMedia media = serviceMediaRepo
                .findByIdAndServiceProfileUserId(mediaId, user.getId())
                .orElseThrow(() -> new RuntimeException("Media not found"));

        awsS3Service.deleteMedia(media.getMediaUrl());

        serviceMediaRepo.delete(media);

        return UserResponse.builder()
                .status(200)
                .message("Media deleted")
                .build();
    }

    @Override
    public UserResponse setMyServiceMediaCover(UUID mediaId) {

        User user = getCurrentUser();

        ServiceMedia target = serviceMediaRepo
                .findByIdAndServiceProfileUserId(mediaId, user.getId())
                .orElseThrow(() -> new RuntimeException("Media not found"));

        List<ServiceMedia> all = serviceMediaRepo
                .findByServiceProfileIdOrderByDisplayOrderAsc(target.getServiceProfile().getId());

        for (ServiceMedia m : all) {
            m.setIsCover(m.getId().equals(mediaId));
        }

        serviceMediaRepo.saveAll(all);

        return UserResponse.builder()
                .status(200)
                .message("Cover updated")
                .build();
    }

    private List<ServiceMediaDto> getServiceMediaDtos(UUID serviceProfileId) {
        return serviceMediaRepo.findByServiceProfileIdOrderByDisplayOrderAsc(serviceProfileId)
                .stream()
                .map(m -> ServiceMediaDto.builder()
                        .id(m.getId())
                        .mediaType(m.getMediaType().name())
                        .mediaUrl(m.getMediaUrl())
                        .isCover(Boolean.TRUE.equals(m.getIsCover()))
                        .build())
                .toList();
    }



    private void validateRegistrationInput(UserDto registrationRequest) {
        registrationRequest.setName(inputSanitizer.sanitize(registrationRequest.getName().trim()));
        registrationRequest.setEmail(inputSanitizer.sanitize(registrationRequest.getEmail().trim().toLowerCase()));
        registrationRequest.setPhoneNumber(inputSanitizer.sanitize(registrationRequest.getPhoneNumber().trim()));

        if (!inputSanitizer.isValidEmail(registrationRequest.getEmail())) {
            throw new RuntimeException("Invalid email format");
        }

        if (inputSanitizer.isMalicious(registrationRequest.getName()) ||
                inputSanitizer.isMalicious(registrationRequest.getEmail()) ||
                inputSanitizer.isMalicious(registrationRequest.getPhoneNumber())) {
            throw new RuntimeException("Invalid input detected");
        }
    }

    private void sanitizeServiceProfileRequest(ServiceProfileRequestDto request) {
        if (request.getBusinessName() != null) request.setBusinessName(inputSanitizer.sanitize(request.getBusinessName().trim()));
        if (request.getTagline() != null) request.setTagline(inputSanitizer.sanitize(request.getTagline().trim()));
        if (request.getDescription() != null) request.setDescription(inputSanitizer.sanitize(request.getDescription().trim()));
        if (request.getCity() != null) request.setCity(inputSanitizer.sanitize(request.getCity().trim()));
        if (request.getState() != null) request.setState(inputSanitizer.sanitize(request.getState().trim()));
        if (request.getCountry() != null) request.setCountry(inputSanitizer.sanitize(request.getCountry().trim()));
        if (request.getZipcode() != null) request.setZipcode(inputSanitizer.sanitize(request.getZipcode().trim()));
        if (request.getServiceArea() != null) request.setServiceArea(inputSanitizer.sanitize(request.getServiceArea().trim()));
        if (request.getAddressLine() != null) request.setAddressLine(inputSanitizer.sanitize(request.getAddressLine().trim()));
        if (request.getLicenseNumber() != null) request.setLicenseNumber(inputSanitizer.sanitize(request.getLicenseNumber().trim()));
        if (request.getWebsiteUrl() != null) request.setWebsiteUrl(inputSanitizer.sanitize(request.getWebsiteUrl().trim()));
        if (request.getWhatsappNumber() != null) request.setWhatsappNumber(inputSanitizer.sanitize(request.getWhatsappNumber().trim()));
    }

    private void validateServiceProfileRequest(ServiceProfileRequestDto request) {
        if (request.getPriceFrom() != null && request.getPriceFrom().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("priceFrom cannot be negative");
        }

        if (request.getPriceTo() != null && request.getPriceTo().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("priceTo cannot be negative");
        }

        if (request.getPriceFrom() != null && request.getPriceTo() != null
                && request.getPriceFrom().compareTo(request.getPriceTo()) > 0) {
            throw new RuntimeException("priceFrom cannot be greater than priceTo");
        }

        if (request.getYearsOfExperience() != null && request.getYearsOfExperience() < 0) {
            throw new RuntimeException("yearsOfExperience cannot be negative");
        }
    }

    private String resolveServiceStatusMessage(ServiceStatus status) {
        return switch (status) {
            case PENDING -> "We are reviewing your application. You can log in, but your service dashboard is not yet available.";
            case ACTIVE -> "Service dashboard retrieved successfully";
            case SUSPENDED -> "Your service account is currently suspended";
            case REJECTED -> "Your service application was rejected";
        };
    }

    private void sendServiceReviewPendingEmail(User user) {
        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "CuttyPaws Service Registration Under Review",
                    "Dear " + user.getName() + ",\n\n" +
                            "Thank you for registering as a service provider on CuttyPaws.\n\n" +
                            "Your application is currently under review. You can still log in to your account, but your service dashboard will remain unavailable until a decision is made.\n\n" +
                            "We will notify you by email once your application has been approved or rejected.\n\n" +
                            "Best regards,\nCuttyPaws Team"
            );
        } catch (Exception e) {
            log.warn("Failed to send pending review email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void notifyAdminOfPendingRegistration(User user, ServiceProfile profile) {
        try {
            emailService.sendEmail(
                    adminReviewEmail,
                    "New Service Registration Waiting for Review",
                    "A new service provider registration is waiting for review.\n\n" +
                            "Name: " + user.getName() + "\n" +
                            "Email: " + user.getEmail() + "\n" +
                            "Phone: " + user.getPhoneNumber() + "\n" +
                            "Service Type: " + profile.getServiceType() + "\n" +
                            "Business Name: " + profile.getBusinessName() + "\n" +
                            "Location: " + profile.getCity() + ", " + profile.getState()
            );
        } catch (Exception e) {
            log.warn("Failed to send admin notification email: {}", e.getMessage());
        }
    }

    private void sendServiceApprovedEmail(User user) {
        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Your CuttyPaws Service Registration Has Been Approved",
                    "Dear " + user.getName() + ",\n\n" +
                            "Congratulations. Your service application has been approved.\n\n" +
                            "You can now access your service dashboard, manage your business profile, and subscribe for advert promotion on CuttyPaws.\n\n" +
                            "Best regards,\nCuttyPaws Team"
            );
        } catch (Exception e) {
            log.warn("Failed to send approval email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private void sendServiceRejectedEmail(User user, String reason) {
        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Your CuttyPaws Service Registration Was Not Approved",
                    "Dear " + user.getName() + ",\n\n" +
                            "We reviewed your service application and we are unable to approve it at this time.\n\n" +
                            "Reason: " + (reason != null && !reason.isBlank() ? reason : "Not specified") + "\n\n" +
                            "You can still log in to your account. You may later update your information and reapply if that flow is enabled.\n\n" +
                            "Best regards,\nCuttyPaws Team"
            );
        } catch (Exception e) {
            log.warn("Failed to send rejection email to {}: {}", user.getEmail(), e.getMessage());
        }
    }
}