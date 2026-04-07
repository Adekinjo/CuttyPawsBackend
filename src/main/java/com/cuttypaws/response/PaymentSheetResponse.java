package com.cuttypaws.response;

import com.cuttypaws.enums.PaymentPurpose;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentSheetResponse {

    private int status;
    private String message;

    private String paymentIntentClientSecret;
    private String paymentIntentId;

    private String customerId;
    private String customerEphemeralKeySecret;

    private String publishableKey;

    private Long paymentId;
    private String reference;
    private BigDecimal amount;
    private String currency;
    private UUID userId;
    private PaymentPurpose paymentPurpose;
    private String paymentStatus;
}