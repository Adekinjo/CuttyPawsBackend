package com.cuttypaws.dto;

import com.cuttypaws.enums.BookingReportReason;
import com.cuttypaws.enums.BookingReportStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ServiceBookingReportDto {
    private UUID id;
    private UUID bookingId;

    private UUID customerId;
    private String customerName;

    private UUID providerUserId;
    private String providerName;

    private UUID serviceProfileId;
    private String businessName;
    private String serviceType;

    private BigDecimal amount;
    private String paymentReference;
    private String petName;
    private String petType;
    private String serviceAddress;
    private String notes;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String timezone;

    private BookingReportReason reason;
    private String details;
    private BookingReportStatus status;
    private String adminNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime resolvedAt;
}