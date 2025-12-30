package com.cuttypaws.repository;

import com.cuttypaws.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepo extends JpaRepository<Review, Long> {

    // Find all reviews by product ID with user name
    @Query("SELECT r FROM Review r JOIN FETCH r.user u WHERE r.product.id = :productId")
    List<Review> findByProductId(@Param("productId") Long productId);

    // Find all reviews by user ID with user name
    @Query("SELECT r FROM Review r JOIN FETCH r.user u WHERE r.user.id = :userId")
    List<Review> findByUserId(@Param("userId") Long userId);

    // Find all reviews by product ID and user ID with user name
    @Query("SELECT r FROM Review r JOIN FETCH r.user u WHERE r.product.id = :productId AND r.user.id = :userId")
    List<Review> findByProductIdAndUserId(@Param("productId") Long productId, @Param("userId") Long userId);

    boolean existsByProductIdAndUserId(Long productId, Long userId);
}