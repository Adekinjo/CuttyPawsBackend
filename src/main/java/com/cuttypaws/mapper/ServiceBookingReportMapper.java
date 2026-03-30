package com.cuttypaws.mapper;

import com.cuttypaws.dto.ServiceBookingReportDto;
import com.cuttypaws.entity.ServiceBooking;
import com.cuttypaws.entity.ServiceBookingReport;
import com.cuttypaws.entity.ServiceProfile;
import org.springframework.stereotype.Component;

@Component
public class ServiceBookingReportMapper {

    public ServiceBookingReportDto toDto(ServiceBookingReport report) {
        if (report == null) return null;

        ServiceBooking booking = report.getBooking();
        ServiceProfile profile = booking != null ? booking.getServiceProfile() : null;

        return ServiceBookingReportDto.builder()
                .id(report.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .customerId(report.getCustomer() != null ? report.getCustomer().getId() : null)
                .customerName(report.getCustomer() != null ? report.getCustomer().getName() : null)
                .providerUserId(report.getProviderUser() != null ? report.getProviderUser().getId() : null)
                .providerName(report.getProviderUser() != null ? report.getProviderUser().getName() : null)
                .serviceProfileId(profile != null ? profile.getId() : null)
                .businessName(profile != null ? profile.getBusinessName() : null)
                .serviceType(booking != null && booking.getServiceType() != null ? booking.getServiceType().name() : null)
                .amount(booking != null ? booking.getAmount() : null)
                .paymentReference(booking != null ? booking.getPaymentReference() : null)
                .petName(booking != null ? booking.getPetName() : null)
                .petType(booking != null ? booking.getPetType() : null)
                .serviceAddress(booking != null ? booking.getServiceAddress() : null)
                .notes(booking != null ? booking.getNotes() : null)
                .startsAt(booking != null ? booking.getStartsAt() : null)
                .endsAt(booking != null ? booking.getEndsAt() : null)
                .timezone(booking != null ? booking.getTimezone() : null)
                .reason(report.getReason())
                .details(report.getDetails())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .reviewedAt(report.getReviewedAt())
                .resolvedAt(report.getResolvedAt())
                .build();
    }
}