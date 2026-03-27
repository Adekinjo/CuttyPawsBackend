package com.cuttypaws.dto;

import com.cuttypaws.enums.PaymentPurpose;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateServiceBookingRequest {

    @NotNull(message = "serviceProfileId is required")
    private UUID serviceProfileId;

    @NotNull(message = "startsAt is required")
    @Future(message = "startsAt must be in the future")
    private LocalDateTime startsAt;

    @NotNull(message = "endsAt is required")
    @Future(message = "endsAt must be in the future")
    private LocalDateTime endsAt;

    @NotNull(message = "amount is required")
    private BigDecimal amount;

    @NotNull(message = "paymentPurpose is required")
    private PaymentPurpose paymentPurpose;

    private String petName;
    private String petType;
    private String serviceAddress;
    private String notes;
    private String timezone;
}