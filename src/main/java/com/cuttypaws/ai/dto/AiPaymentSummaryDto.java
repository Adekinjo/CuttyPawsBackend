package com.cuttypaws.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AiPaymentSummaryDto {
    private Long paymentId;
    private String reference;
    private String transactionId;
    private String checkoutSessionId;
    private String status;
    private String currency;
    private BigDecimal amount;
    private String paymentPurpose;
}