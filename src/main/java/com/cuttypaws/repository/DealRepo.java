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

    @Query("SELECT d FROM Deal d WHERE d.active = true AND " +
            "d.startDate <= :now AND d.endDate >= :now")
    List<Deal> findActiveDeals(LocalDateTime now);
}