package com.cuttypaws.repository;

import com.cuttypaws.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepo extends JpaRepository<OrderItem, Long>, JpaSpecificationExecutor<OrderItem> {
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.id IN :productIds")
    Page<OrderItem> findByProductIdIn(@Param("productIds") List<Long> productIds, Pageable pageable);
}
