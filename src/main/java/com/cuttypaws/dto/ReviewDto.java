package com.cuttypaws.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewDto {
    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime timestamp;
    private Long productId;
    private UUID userId;
    private String userName;
}


