package com.cuttypaws.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ServiceMediaDto {

    private UUID id;

    private String mediaType;

    private String mediaUrl;

    private Boolean isCover;

    public UUID serviceProfileId;
}