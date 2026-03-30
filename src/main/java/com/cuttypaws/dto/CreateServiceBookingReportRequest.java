package com.cuttypaws.dto;

import com.cuttypaws.enums.BookingReportReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateServiceBookingReportRequest {

    @NotNull
    private UUID bookingId;

    @NotNull
    private BookingReportReason reason;

    @NotBlank
    private String details;
}