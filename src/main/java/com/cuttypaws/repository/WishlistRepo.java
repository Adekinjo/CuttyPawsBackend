package com.cuttypaws.repository;

import com.cuttypaws.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistRepo extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserId(UUID userId);
    Optional<Wishlist> findByUserIdAndProductId(UUID userId, Long productId);

    @Modifying
    @Query("DELETE FROM Wishlist w WHERE w.user.id = :userId AND w.product.id = :productId")
    void deleteByUserIdAndProductId(UUID userId, Long productId);
}
