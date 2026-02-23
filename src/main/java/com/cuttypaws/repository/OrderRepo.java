package com.cuttypaws.repository;

import com.cuttypaws.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface OrderRepo extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"orderItemList", "orderItemList.product"})
    Page<Order> findByUserId(UUID userId, Pageable pageable);
}

