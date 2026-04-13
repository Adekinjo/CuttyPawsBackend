package com.cuttypaws.feed.controller;

import com.cuttypaws.feed.dto.FeedResponseDto;
import com.cuttypaws.feed.dto.VideoFeedPageResponse;
import com.cuttypaws.feed.service.interf.FeedComposerService;
import com.cuttypaws.feed.service.interf.VideoFeedService;
import com.cuttypaws.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
@Slf4j
public class FeedController {

    private final FeedComposerService feedComposerService;
    private final VideoFeedService videoFeedService;

    @GetMapping("/mixed")
    public ResponseEntity<FeedResponseDto> getMixedFeed(
            Authentication authentication,
            @RequestParam(required = false) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "8") int limit
    ) {
        UUID currentUserId = extractCurrentUserId(authentication);

        return ResponseEntity.ok(
                feedComposerService.getMixedFeed(currentUserId, cursorCreatedAt, cursorId, limit)
        );
    }

    @GetMapping("/videos")
    public ResponseEntity<VideoFeedPageResponse> getVideoFeed(
            Authentication authentication,
            @RequestParam(required = false) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        UUID currentUserId = extractCurrentUserId(authentication);

        return ResponseEntity.ok(
                videoFeedService.getVideoFeed(currentUserId, cursorCreatedAt, cursorId, limit)
        );
    }

    private UUID extractCurrentUserId(Authentication authentication) {
        try {
            if (authentication == null ||
                    !authentication.isAuthenticated() ||
                    authentication instanceof AnonymousAuthenticationToken) {
                return null;
            }

            Object principal = authentication.getPrincipal();

            if (principal instanceof AuthUser authUser && authUser.getUser() != null) {
                return authUser.getUser().getId();
            }
        } catch (Exception e) {
            log.warn("Failed to extract current user id for public feed request", e);
        }

        return null;
    }
}