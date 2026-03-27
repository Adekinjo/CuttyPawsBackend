package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaDto {
    private String url;
    private String type; // IMAGE/VIDEO
    private String thumbnailUrl;

    private String streamUrl;
    private Integer durationSeconds;
    private Boolean processed;
}
