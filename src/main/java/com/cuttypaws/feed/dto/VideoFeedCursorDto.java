package com.cuttypaws.feed.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFeedCursorDto {
    private LocalDateTime cursorCreatedAt;
    private Long cursorId;
}