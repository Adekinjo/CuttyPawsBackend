package com.cuttypaws.repository;

import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.enums.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceAdSubscriptionRepo extends JpaRepository<ServiceAdSubscription, UUID> {

    List<ServiceAdSubscription> findByServiceProfileIdOrderByCreatedAtDesc(UUID serviceProfileId);

    List<ServiceAdSubscription> findByServiceProfileUserIdOrderByCreatedAtDesc(UUID userId);

    List<ServiceAdSubscription> findByPaymentStatus(PaymentStatus paymentStatus);

    Optional<ServiceAdSubscription> findFirstByServiceProfileIdAndIsActiveTrueAndEndsAtAfterOrderByCreatedAtDesc(
            UUID serviceProfileId,
            LocalDateTime now
    );

    @EntityGraph(attributePaths = {
            "serviceProfile",
            "serviceProfile.user"
    })
    List<ServiceAdSubscription> findByIsActiveTrueAndEndsAtAfterAndPaymentStatusOrderByCreatedAtDesc(
            LocalDateTime now,
            PaymentStatus paymentStatus
    );

    List<ServiceAdSubscription> findByIsActiveTrueAndEndsAtBefore(LocalDateTime now);

    Optional<ServiceAdSubscription> findByPaymentReference(String paymentReference);

    Optional<ServiceAdSubscription> findByCheckoutSessionId(String checkoutSessionId);
}