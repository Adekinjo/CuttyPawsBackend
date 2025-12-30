package com.cuttypaws.repository;

import com.cuttypaws.entity.ProductColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductColorRepo extends JpaRepository<ProductColor, Long> {

    // Find all colors for a specific product
    List<ProductColor> findByProductId(Long productId);

    // Find a specific color by ID
    Optional<ProductColor> findById(Long id);
}