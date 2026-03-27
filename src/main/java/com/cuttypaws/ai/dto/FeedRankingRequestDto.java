package com.cuttypaws.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FeedRankingRequestDto {
    private UUID userId;
    private String userCity;
    private String userState;
    private String primaryPetType;
    private List<String> recentSearches;
    private List<String> likedCategories;
    private List<FeedRankingCandidateDto> candidates;
}