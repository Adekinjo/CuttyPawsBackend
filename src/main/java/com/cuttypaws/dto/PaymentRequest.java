package com.cuttypaws.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentRequest {
    private BigDecimal amount;
    private String email;
    private String currency;
    private UUID userId;
    private Long orderId;
    private String transactionId;
    private String method;
    private String reference;
}

