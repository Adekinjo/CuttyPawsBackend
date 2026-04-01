package com.cuttypaws.repository;

import com.cuttypaws.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushTokenRepo extends JpaRepository<PushToken, Long> {

    List<PushToken> findByUserIdAndActiveTrue(UUID userId);

    Optional<PushToken> findByToken(String token);
}
