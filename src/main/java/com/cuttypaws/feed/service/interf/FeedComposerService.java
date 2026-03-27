package com.cuttypaws.feed.service.interf;

import com.cuttypaws.feed.dto.FeedResponseDto;

import java.util.UUID;

public interface FeedComposerService {
    FeedResponseDto getMixedFeed(UUID currentUserId, int limit);
}