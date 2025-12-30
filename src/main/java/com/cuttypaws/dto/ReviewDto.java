package com.cuttypaws.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewDto {
    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime timestamp;
    private Long productId;
    private Long userId;
    private String userName;
}


