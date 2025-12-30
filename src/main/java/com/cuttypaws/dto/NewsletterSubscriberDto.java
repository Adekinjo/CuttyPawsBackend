package com.cuttypaws.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsletterSubscriberDto {
    private Long id;
    private String email;
    private Boolean isActive;
    private Boolean subscribed;
    private LocalDateTime createdAt;
}
