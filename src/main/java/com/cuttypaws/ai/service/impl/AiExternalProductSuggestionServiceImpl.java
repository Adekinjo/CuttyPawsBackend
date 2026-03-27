package com.cuttypaws.ai.service.impl;

import com.cuttypaws.ai.dto.ExternalProductSuggestionDto;
import com.cuttypaws.ai.service.interf.AiExternalProductSuggestionService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AiExternalProductSuggestionServiceImpl implements AiExternalProductSuggestionService {

    @Override
    public List<ExternalProductSuggestionDto> searchExternalProducts(List<String> keywords) {
        return Collections.emptyList();
    }
}