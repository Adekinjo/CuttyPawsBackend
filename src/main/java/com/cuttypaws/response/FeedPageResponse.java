package com.cuttypaws.response;

import com.cuttypaws.dto.FeedCursor;
import com.cuttypaws.dto.PostDto;

import java.util.List;

public record FeedPageResponse(
        List<PostDto> posts,
        FeedCursor nextCursor,
        boolean hasMore
) {}