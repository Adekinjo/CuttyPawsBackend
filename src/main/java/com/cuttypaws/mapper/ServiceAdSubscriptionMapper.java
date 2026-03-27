package com.cuttypaws.mapper;

import com.cuttypaws.dto.ServiceAdSubscriptionDto;
import com.cuttypaws.entity.ServiceAdSubscription;
import org.springframework.stereotype.Component;

@Component
public class ServiceAdSubscriptionMapper {

    public ServiceAdSubscriptionDto toDto(ServiceAdSubscription subscription) {
        if (subscription == null) {
            return null;
        }

        return ServiceAdSubscriptionDto.builder()
                .id(subscription.getId())
                .serviceProfileId(subscription.getServiceProfile() != null ? subscription.getServiceProfile().getId() : null)
                .planType(subscription.getPlanType())
                .amount(subscription.getAmount())
                .paymentStatus(subscription.getPaymentStatus())
                .paymentReference(subscription.getPaymentReference())
                .paymentUrl(subscription.getPaymentUrl())
                .isActive(subscription.getIsActive())
                .startsAt(subscription.getStartsAt())
                .endsAt(subscription.getEndsAt())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}