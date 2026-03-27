package com.cuttypaws.ai.service.interf;

import com.cuttypaws.ai.dto.ExternalProductSuggestionDto;
import com.cuttypaws.dto.ProductDto;
import com.cuttypaws.dto.ServiceProfileDto;

import java.util.List;

public interface AiRecommendationService {
    List<ProductDto> recommendProducts(String query);
    List<ServiceProfileDto> recommendServices(String serviceType, String city, String state);
    List<ExternalProductSuggestionDto> recommendExternalProducts(List<String> keywords);
}