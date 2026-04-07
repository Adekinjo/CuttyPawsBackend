package com.cuttypaws.dto;

import com.cuttypaws.enums.PaymentPurpose;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PaymentSheetRequest {
    private BigDecimal amount;
    private String email;
    private String currency;
    private UUID userId;
    private PaymentPurpose paymentPurpose;
    private UUID serviceAdSubscriptionId;
    private UUID serviceBookingId;
    private String platform;
}