package com.cuttypaws.feed.service.interf;

import com.cuttypaws.feed.dto.VideoFeedPageResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VideoFeedService {
    VideoFeedPageResponse getVideoFeed(
            UUID currentUserId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit
    );
}