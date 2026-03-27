package com.cuttypaws.feed.dto;

import com.cuttypaws.dto.PostDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFeedPageResponse {
    private List<PostDto> items;
    private VideoFeedCursorDto nextCursor;
    private boolean hasMore;
}