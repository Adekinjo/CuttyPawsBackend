// DealRepository.java
package com.cuttypaws.repository;

import com.cuttypaws.entity.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DealRepo extends JpaRepository<Deal, Long> {
    List<Deal> findByActiveTrueAndEndDateAfter(LocalDateTime now);
    Optional<Deal> findByProductId(Long productId);

}