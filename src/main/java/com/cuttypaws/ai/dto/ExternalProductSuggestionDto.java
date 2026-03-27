package com.cuttypaws.ai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExternalProductSuggestionDto {
    private String title;
    private String description;
    private String sourceName;
    private String sourceUrl;
    private String imageUrl;
}