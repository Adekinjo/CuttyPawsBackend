package com.cuttypaws.repository;

import com.cuttypaws.entity.Payment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReference(String reference);
    Optional<Payment> findByPaymentIntentId(String paymentIntentId);
    List<Payment> findByUserIdOrderByIdDesc(UUID userId, Pageable pageable);
}