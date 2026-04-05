package com.cuttypaws.repository;

import com.cuttypaws.entity.ServiceAdSubscription;
import com.cuttypaws.enums.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceAdSubscriptionRepo extends JpaRepository<ServiceAdSubscription, UUID> {

    List<ServiceAdSubscription> findByServiceProfileUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ServiceAdSubscription> findFirstByServiceProfileIdAndIsActiveTrueAndEndsAtAfterOrderByCreatedAtDesc(
            UUID serviceProfileId,
            LocalDateTime now
    );

    List<ServiceAdSubscription> findByIsActiveTrueAndEndsAtBefore(LocalDateTime now);

    Optional<ServiceAdSubscription> findByPaymentReference(String paymentReference);

    Optional<ServiceAdSubscription> findByCheckoutSessionId(String checkoutSessionId);

    @Query("""
    SELECT s
    FROM ServiceAdSubscription s
    WHERE s.isActive = true
      AND s.endsAt > :now
      AND s.paymentStatus = :paymentStatus
    ORDER BY s.createdAt DESC
""")
    List<ServiceAdSubscription> findFeedAdCandidates(
            @Param("now") LocalDateTime now,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            org.springframework.data.domain.Pageable pageable
    );
}