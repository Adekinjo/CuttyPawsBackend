package com.cuttypaws.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmServiceAdPaymentRequest {

    @NotBlank(message = "Payment reference is required")
    private String paymentReference;

    private String gatewayStatus;
}