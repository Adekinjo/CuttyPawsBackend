package com.cuttypaws.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UnifiedAiSearchRequest {

    @NotBlank(message = "Query is required")
    private String query;

    private Double latitude;
    private Double longitude;
    private Integer radiusMiles;
}