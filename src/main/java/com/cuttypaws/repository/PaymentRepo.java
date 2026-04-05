package com.cuttypaws.repository;

import com.cuttypaws.entity.Payment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReference(String reference);
    Optional<Payment> findByCheckoutSessionId(String checkoutSessionId);
    List<Payment> findByUserIdOrderByIdDesc(UUID userId, Pageable pageable);

}