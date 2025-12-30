package com.cuttypaws.repository;

import com.cuttypaws.entity.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSizeRepo extends JpaRepository<ProductSize, Long> {

    // Find all sizes for a specific product
    List<ProductSize> findByProductId(Long productId);

    // Find a specific size by ID
    Optional<ProductSize> findById(Long id);
}