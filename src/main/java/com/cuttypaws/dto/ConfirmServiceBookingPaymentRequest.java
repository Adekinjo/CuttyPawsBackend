package com.cuttypaws.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmServiceBookingPaymentRequest {
    @NotBlank(message = "paymentReference is required")
    private String paymentReference;
}