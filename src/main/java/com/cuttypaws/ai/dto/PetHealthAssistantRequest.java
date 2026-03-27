package com.cuttypaws.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PetHealthAssistantRequest {

    @NotBlank
    private String question;

    // later: multipart upload support
    private String imageUrl;
}