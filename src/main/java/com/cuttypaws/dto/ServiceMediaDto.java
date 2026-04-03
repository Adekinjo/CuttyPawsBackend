package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMediaDto {

    private UUID id;

    private String mediaType;

    private String mediaUrl;

    private Boolean isCover;

    public UUID serviceProfileId;
}