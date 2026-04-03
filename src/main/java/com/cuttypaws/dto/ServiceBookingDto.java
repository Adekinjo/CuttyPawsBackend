package com.cuttypaws.dto;

import com.cuttypaws.enums.BookingStatus;
import com.cuttypaws.enums.PaymentStatus;
import com.cuttypaws.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBookingDto {
    private UUID id;

    private UUID customerId;
    private String customerName;

    private UUID serviceProfileId;
    private UUID providerUserId;
    private String providerName;
    private String businessName;
    private ServiceType serviceType;

    private String petName;
    private String petType;
    private String serviceAddress;
    private String notes;

    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String timezone;

    private BigDecimal amount;

    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private String paymentReference;
    private String paymentUrl;

    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}