package com.cuttypaws.feed.controller;

import com.cuttypaws.feed.dto.FeedResponseDto;
import com.cuttypaws.feed.dto.VideoFeedPageResponse;
import com.cuttypaws.feed.service.interf.FeedComposerService;
import com.cuttypaws.feed.service.interf.VideoFeedService;
import com.cuttypaws.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedComposerService feedComposerService;
    private final VideoFeedService videoFeedService;

    @GetMapping("/mixed")
    public ResponseEntity<FeedResponseDto> getMixedFeed(
            @CurrentUser UUID currentUserId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(feedComposerService.getMixedFeed(currentUserId, limit));
    }

    @GetMapping("/videos")
    public ResponseEntity<VideoFeedPageResponse> getVideoFeed(
            @CurrentUser UUID currentUserId,
            @RequestParam(required = false) LocalDateTime cursorCreatedAt,
            @RequestParam(required = false) Long cursorId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(
                videoFeedService.getVideoFeed(currentUserId, cursorCreatedAt, cursorId, limit)
        );
    }
}