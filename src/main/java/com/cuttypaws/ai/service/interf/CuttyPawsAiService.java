package com.cuttypaws.ai.service.interf;

import com.cuttypaws.ai.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CuttyPawsAiService {

    AiChatResponse chat(AiChatRequest request);

    AiChatResponse petHealthAdvice(PetHealthAssistantRequest request);

    AiChatResponse petHealthImageAdvice(String question, MultipartFile image);

    UnifiedSearchResultDto parseUnifiedSearch(UnifiedAiSearchRequest request);

    AiChatResponse aiHelp(AiChatRequest request);

    AiChatResponse aiHelpWithImage(UUID userId, String prompt, MultipartFile image);
}