package com.cuttypaws.ai.dto;

import com.cuttypaws.feed.enums.FeedItemType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FeedRankingCandidateDto {
    private String candidateId;
    private FeedItemType type;

    private UUID ownerId;
    private String captionOrDescription;
    private String category;
    private String serviceType;
    private String petType;
    private Boolean sponsored;

    private List<String> tags;
}