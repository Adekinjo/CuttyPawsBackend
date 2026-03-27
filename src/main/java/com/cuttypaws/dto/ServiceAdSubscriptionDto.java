package com.cuttypaws.dto;

import com.cuttypaws.enums.AdPlanType;
import com.cuttypaws.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ServiceAdSubscriptionDto {
    private UUID id;
    private UUID serviceProfileId;
    private AdPlanType planType;
    private BigDecimal amount;
    private PaymentStatus paymentStatus;
    private String paymentReference;
    private String paymentProvider;
    private String paymentUrl;
    private Boolean isActive;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}