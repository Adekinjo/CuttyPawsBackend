package com.cuttypaws.repository;

import com.cuttypaws.entity.ServiceBooking;
import com.cuttypaws.enums.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceBookingRepo extends JpaRepository<ServiceBooking, UUID> {

    @EntityGraph(attributePaths = {"customer", "serviceProfile", "serviceProfile.user"})
    List<ServiceBooking> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @EntityGraph(attributePaths = {"customer", "serviceProfile", "serviceProfile.user"})
    List<ServiceBooking> findByServiceProfileUserIdOrderByCreatedAtDesc(UUID providerUserId);

    @EntityGraph(attributePaths = {"customer", "serviceProfile", "serviceProfile.user"})
    Optional<ServiceBooking> findByPaymentReference(String paymentReference);

    boolean existsByServiceProfileIdAndBookingStatusInAndStartsAtLessThanAndEndsAtGreaterThan(
            UUID serviceProfileId,
            Collection<BookingStatus> statuses,
            LocalDateTime endsAt,
            LocalDateTime startsAt
    );
}