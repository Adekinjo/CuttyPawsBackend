package com.cuttypaws.repository;

import com.cuttypaws.entity.CheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutSessionRepo extends JpaRepository<CheckoutSession, Long> {
}