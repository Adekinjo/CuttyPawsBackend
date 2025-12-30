package com.cuttypaws.repository;

import com.cuttypaws.entity.CustomerSupport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Repository: CustomerSupportQueryRepository
@Repository
public interface CustomerSupportRepo extends JpaRepository<CustomerSupport, Long> {
    List<CustomerSupport> findByCustomerId(String customerId);
}

