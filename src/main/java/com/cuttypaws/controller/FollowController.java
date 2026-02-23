package com.cuttypaws.controller;

import com.cuttypaws.response.FollowResponse;
import com.cuttypaws.service.interf.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/follow")
@RequiredArgsConstructor
@Slf4j
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{userId}")
    public ResponseEntity<FollowResponse> followUser(@PathVariable UUID userId) {
        log.info("🎯 FOLLOW USER REQUEST - Target User ID: {}", userId);
        FollowResponse response = followService.followUser(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<FollowResponse> unfollowUser(@PathVariable UUID userId) {
        log.info("🎯 UNFOLLOW USER REQUEST - Target User ID: {}", userId);
        FollowResponse response = followService.unfollowUser(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<FollowResponse> getFollowStats(@PathVariable UUID userId) {
        log.info("🎯 GET FOLLOW STATS REQUEST - User ID: {}", userId);
        FollowResponse response = followService.getFollowStats(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<FollowResponse> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("🎯 GET FOLLOWERS REQUEST - User ID: {}, Page: {}, Size: {}", userId, page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        FollowResponse response = followService.getFollowers(userId, pageable);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<FollowResponse> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("🎯 GET FOLLOWING REQUEST - User ID: {}, Page: {}, Size: {}", userId, page, size);

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        FollowResponse response = followService.getFollowing(userId, pageable);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<FollowResponse> checkFollowStatus(@PathVariable UUID userId) {
        log.info("🎯 CHECK FOLLOW STATUS REQUEST - Target User ID: {}", userId);
        FollowResponse response = followService.checkFollowStatus(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/{userId}/mute")
    public ResponseEntity<FollowResponse> muteUser(@PathVariable UUID userId) {
        log.info("🎯 MUTE USER REQUEST - Target User ID: {}", userId);
        FollowResponse response = followService.muteUser(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/{userId}/unmute")
    public ResponseEntity<FollowResponse> unmuteUser(@PathVariable UUID userId) {
        log.info("🎯 UNMUTE USER REQUEST - Target User ID: {}", userId);
        FollowResponse response = followService.unmuteUser(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/{userId}/mutual")
    public ResponseEntity<FollowResponse> getMutualFollowers(@PathVariable UUID userId) {
        log.info("🎯 GET MUTUAL FOLLOWERS REQUEST - Target User ID: {}", userId);
        FollowResponse response = followService.getMutualFollowers(userId);
        return ResponseEntity.status(response.getStatus()).body(response);
    }
}