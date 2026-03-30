package com.cuttypaws.repository;

import com.cuttypaws.entity.ServiceBookingReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceBookingReportRepo extends JpaRepository<ServiceBookingReport, UUID> {

    @EntityGraph(attributePaths = {"booking", "booking.serviceProfile", "booking.serviceProfile.user", "customer", "providerUser"})
    List<ServiceBookingReport> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    @EntityGraph(attributePaths = {"booking", "booking.serviceProfile", "booking.serviceProfile.user", "customer", "providerUser"})
    List<ServiceBookingReport> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"booking", "booking.serviceProfile", "booking.serviceProfile.user", "customer", "providerUser"})
    Optional<ServiceBookingReport> findById(UUID id);

    boolean existsByBookingIdAndCustomerId(UUID bookingId, UUID customerId);
}