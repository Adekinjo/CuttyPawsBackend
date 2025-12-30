package com.cuttypaws.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private BigDecimal amount;
    private String email;
    private String currency;
    private Long userId;
    private Long orderId;
    private String transactionId;
    private String method;
    private String reference;
}

