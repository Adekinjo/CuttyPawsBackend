package com.cuttypaws.ai.dto;

import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.ServiceProfileDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiChatResponse {
    private String answer;
    private String model;
    private boolean success;
    private String disclaimer;
    private String feature;

    private String recommendationType; // NONE, PRODUCT, SERVICE, BOTH
    private List<ProductDto> recommendedProducts;
    private List<ServiceProfileDto> recommendedServices;
    private List<ExternalProductSuggestionDto> externalProductSuggestions;
    private List<String> followUpQuestions;
}