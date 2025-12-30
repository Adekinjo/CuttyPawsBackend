package com.cuttypaws.repository;

import com.cuttypaws.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepo extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReference(String reference);
    Optional<Payment> findByTransactionId(String transactionId);
}

