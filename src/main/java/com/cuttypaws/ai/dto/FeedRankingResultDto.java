package com.cuttypaws.ai.dto;

import lombok.Data;

@Data
public class FeedRankingResultDto {
    private String candidateId;
    private Double score;
}