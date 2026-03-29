package com.cuttypaws.feed.service.interf;

import com.cuttypaws.feed.dto.FeedResponseDto;

import java.time.LocalDateTime;
import java.util.UUID;

public interface FeedComposerService {
    FeedResponseDto getMixedFeed(
            UUID currentUserId,
            LocalDateTime cursorCreatedAt,
            Long cursorId,
            int limit
    );
}