package com.cuttypaws.ai.service.interf;

import com.cuttypaws.ai.dto.FeedRankingRequestDto;
import com.cuttypaws.ai.dto.FeedRankingResultDto;

import java.util.List;

public interface AiFeedRankingService {
    List<FeedRankingResultDto> rankFeed(FeedRankingRequestDto request);
}