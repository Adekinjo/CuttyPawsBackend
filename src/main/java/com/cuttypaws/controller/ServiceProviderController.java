package com.cuttypaws.controller;

import com.cuttypaws.dto.ServiceProfileRequestDto;
import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.interf.ServiceProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
public class ServiceProviderController {

    private final ServiceProviderService serviceProviderService;

    @PutMapping("/my-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateMyServiceProfile(@Valid @RequestBody ServiceProfileRequestDto request) {
        UserResponse response = serviceProviderService.updateMyServiceProfile(request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/my-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyServiceProfile() {
        UserResponse response = serviceProviderService.getMyServiceProfile();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/my-dashboard")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMyServiceDashboard() {
        UserResponse response = serviceProviderService.getMyServiceDashboard();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/public/{userId}")
    public ResponseEntity<UserResponse> getPublicServiceProfile(@PathVariable UUID userId) {
        UserResponse response = serviceProviderService.getPublicServiceProfile(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/admin/pending")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> getPendingServiceRegistrations() {
        UserResponse response = serviceProviderService.getPendingServiceRegistrations();
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/admin/{userId}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> approveServiceRegistration(@PathVariable UUID userId) {
        UserResponse response = serviceProviderService.approveServiceRegistration(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/admin/{userId}/reject")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> rejectServiceRegistration(
            @PathVariable UUID userId,
            @RequestParam(required = false) String reason
    ) {
        UserResponse response = serviceProviderService.rejectServiceRegistration(userId, reason);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping(value = "/my-profile/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_SERVICE_PROVIDER')")
    public ResponseEntity<UserResponse> uploadMyServiceMedia(
            @RequestPart("files") java.util.List<MultipartFile> files
    ) {
        UserResponse response = serviceProviderService.uploadMyServiceMedia(files);
        return ResponseEntity.status(response.getStatus()).body(response);
    }


    @GetMapping("/my-profile/media")
    @PreAuthorize("hasAuthority('ROLE_SERVICE_PROVIDER')")
    public ResponseEntity<UserResponse> getMyServiceMedia() {

        UserResponse response = serviceProviderService.getMyServiceMedia();

        return ResponseEntity.status(response.getStatus()).body(response);
    }


    @DeleteMapping("/my-profile/media/{mediaId}")
    @PreAuthorize("hasAuthority('ROLE_SERVICE_PROVIDER')")
    public ResponseEntity<UserResponse> deleteMyServiceMedia(@PathVariable UUID mediaId) {

        UserResponse response = serviceProviderService.deleteMyServiceMedia(mediaId);

        return ResponseEntity.status(response.getStatus()).body(response);
    }


    @PostMapping("/my-profile/media/{mediaId}/cover")
    @PreAuthorize("hasAuthority('ROLE_SERVICE_PROVIDER')")
    public ResponseEntity<UserResponse> setMyServiceMediaCover(@PathVariable UUID mediaId) {

        UserResponse response = serviceProviderService.setMyServiceMediaCover(mediaId);

        return ResponseEntity.status(response.getStatus()).body(response);
    }
}