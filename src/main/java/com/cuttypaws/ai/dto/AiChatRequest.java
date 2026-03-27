package com.cuttypaws.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AiChatRequest {
    private UUID userId;

    @NotBlank(message = "Prompt is required")
    private String prompt;

    private String feature; // GENERAL, FEED, SEARCH, PET_HEALTH, AI_SUPPORT
    private List<String> imageUrls;
    private String conversationId;

    private String city;
    private String state;
    private Integer radiusMiles;
}