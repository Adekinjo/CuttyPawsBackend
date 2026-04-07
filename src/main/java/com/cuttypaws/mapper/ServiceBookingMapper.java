package com.cuttypaws.mapper;

import com.cuttypaws.dto.ServiceBookingDto;
import com.cuttypaws.entity.Payment;
import com.cuttypaws.entity.ServiceBooking;
import org.springframework.stereotype.Component;

@Component
public class ServiceBookingMapper {

    public ServiceBookingDto toDto(ServiceBooking booking) {
        return toDto(booking, null);
    }

    public ServiceBookingDto toDto(ServiceBooking booking, Payment payment) {
        if (booking == null) {
            return null;
        }

        return ServiceBookingDto.builder()
                .id(booking.getId())
                .customerId(booking.getCustomer() != null ? booking.getCustomer().getId() : null)
                .customerName(booking.getCustomer() != null ? booking.getCustomer().getName() : null)
                .serviceProfileId(booking.getServiceProfile() != null ? booking.getServiceProfile().getId() : null)
                .providerUserId(
                        booking.getServiceProfile() != null && booking.getServiceProfile().getUser() != null
                                ? booking.getServiceProfile().getUser().getId()
                                : null
                )
                .providerName(
                        booking.getServiceProfile() != null && booking.getServiceProfile().getUser() != null
                                ? booking.getServiceProfile().getUser().getName()
                                : null
                )
                .businessName(booking.getServiceProfile() != null ? booking.getServiceProfile().getBusinessName() : null)
                .serviceType(booking.getServiceType())
                .petName(booking.getPetName())
                .petType(booking.getPetType())
                .serviceAddress(booking.getServiceAddress())
                .notes(booking.getNotes())
                .startsAt(booking.getStartsAt())
                .endsAt(booking.getEndsAt())
                .timezone(booking.getTimezone())
                .amount(booking.getAmount())
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .paymentReference(booking.getPaymentReference())
                .confirmedAt(booking.getConfirmedAt())
                .cancelledAt(booking.getCancelledAt())
                .completedAt(booking.getCompletedAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}