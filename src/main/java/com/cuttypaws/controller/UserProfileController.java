package com.cuttypaws.controller;

import com.cuttypaws.response.UserResponse;
import com.cuttypaws.service.interf.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable Long userId) {
        log.info("🎯 GET USER PROFILE REQUEST - User ID: {}", userId);
        UserResponse response = userProfileService.getUserProfile(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{userId}/posts")
    public ResponseEntity<UserResponse> getUserPosts(@PathVariable Long userId) {
        log.info("🎯 GET USER POSTS REQUEST - User ID: {}", userId);
        UserResponse response = userProfileService.getUserPosts(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserResponse> getUserStats(@PathVariable Long userId) {
        log.info("🎯 GET USER STATS REQUEST - User ID: {}", userId);
        UserResponse response = userProfileService.getUserStats(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/{userId}/block")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> blockUser(
            @PathVariable Long userId,
            @RequestParam String reason
    ) {
        log.info("🚫 BLOCK USER REQUEST - User ID: {}, Reason: {}", userId, reason);
        UserResponse response = userProfileService.blockUser(userId, reason);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/{userId}/unblock")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserResponse> unblockUser(@PathVariable Long userId) {
        log.info("✅ UNBLOCK USER REQUEST - User ID: {}", userId);
        UserResponse response = userProfileService.unblockUser(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}