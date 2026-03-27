package com.cuttypaws.ai.controller;

import com.cuttypaws.ai.dto.*;
import com.cuttypaws.ai.service.interf.CuttyPawsAiService;
import com.cuttypaws.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final CuttyPawsAiService cuttyPawsAiService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @CurrentUser UUID currentUserId,
            @RequestBody @Valid AiChatRequest request
    ) {
        request.setUserId(currentUserId);
        return ResponseEntity.ok(cuttyPawsAiService.chat(request));
    }

    @PostMapping("/search/parse")
    public ResponseEntity<UnifiedSearchResultDto> parseSearch(
            @RequestBody @Valid UnifiedAiSearchRequest request
    ) {
        return ResponseEntity.ok(cuttyPawsAiService.parseUnifiedSearch(request));
    }

    @PostMapping("/pet-health")
    public ResponseEntity<AiChatResponse> petHealth(
            @RequestBody @Valid PetHealthAssistantRequest request
    ) {
        return ResponseEntity.ok(cuttyPawsAiService.petHealthAdvice(request));
    }

    @PostMapping(value = "/pet-health/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiChatResponse> petHealthImage(
            @RequestParam("question") String question,
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(cuttyPawsAiService.petHealthImageAdvice(question, image));
    }

    @PostMapping("/support")
    public ResponseEntity<AiChatResponse> support(
            @CurrentUser UUID currentUserId,
            @RequestBody @Valid AiChatRequest request
    ) {
        request.setUserId(currentUserId);
        return ResponseEntity.ok(cuttyPawsAiService.aiHelp(request));
    }

    @PostMapping(value = "/support/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AiChatResponse> supportWithImage(
            @CurrentUser UUID currentUserId,
            @RequestParam("prompt") String prompt,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "state", required = false) String state,
            @RequestPart("image") MultipartFile image
    ) {
        AiChatResponse response = cuttyPawsAiService.aiHelpWithImage(currentUserId, prompt, image);
        return ResponseEntity.ok(response);
    }
}