package com.cuttypaws.dto;

import com.cuttypaws.enums.PaymentPurpose;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentSheetRequest {
    private Long checkoutSessionId;
    private PaymentPurpose paymentPurpose;
    private UUID serviceAdSubscriptionId;
    private UUID serviceBookingId;
    private String platform;
}