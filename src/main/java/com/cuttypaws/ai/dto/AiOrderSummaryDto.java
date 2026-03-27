package com.cuttypaws.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AiOrderSummaryDto {
    private Long orderId;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private Integer itemCount;
}