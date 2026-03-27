package com.cuttypaws.ai.service.interf;

import com.cuttypaws.ai.dto.ExternalProductSuggestionDto;

import java.util.List;

public interface AiExternalProductSuggestionService {
    List<ExternalProductSuggestionDto> searchExternalProducts(List<String> keywords);
}