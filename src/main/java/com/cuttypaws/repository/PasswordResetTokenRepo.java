package com.cuttypaws.repository;

import com.cuttypaws.entity.PasswordResetToken;
import com.cuttypaws.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByUser(User user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiryDate < :now OR (t.used = true AND t.usedAt < :cutoffTime)")
    void deleteExpiredOrOldUsedTokens(@Param("now") LocalDateTime now,
                                      @Param("cutoffTime") LocalDateTime cutoffTime);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.user = :user")
    void deleteByUser(@Param("user") User user);

    List<PasswordResetToken> findByUsedTrueAndUsedAtBefore(LocalDateTime cutoffTime);
}