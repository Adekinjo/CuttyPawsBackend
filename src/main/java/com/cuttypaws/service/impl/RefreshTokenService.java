package com.cuttypaws.service.impl;

import com.cuttypaws.entity.RefreshToken;
import com.cuttypaws.entity.User;
import com.cuttypaws.repository.RefreshTokenRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepo refreshTokenRepo;

    @Transactional
    public RefreshToken saveToken(User user, String token, boolean rememberMe, LocalDateTime expiresAt) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .rememberMe(rememberMe)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        return refreshTokenRepo.save(refreshToken);
    }

    public RefreshToken getByToken(String token) {
        return refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));
    }

    @Transactional
    public void revokeToken(RefreshToken token, String replacedByToken) {
        token.setRevoked(true);
        token.setRevokedAt(LocalDateTime.now());
        token.setReplacedByToken(replacedByToken);
        refreshTokenRepo.save(token);
    }

    @Transactional
    public void revokeAllActiveTokens(User user) {
        List<RefreshToken> activeTokens = refreshTokenRepo.findByUserAndRevokedFalse(user);
        for (RefreshToken token : activeTokens) {
            token.setRevoked(true);
            token.setRevokedAt(LocalDateTime.now());
        }
        refreshTokenRepo.saveAll(activeTokens);
    }

    @Transactional
    public void deleteExpiredAndRevokedTokens() {
        List<RefreshToken> allTokens = refreshTokenRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        List<RefreshToken> toDelete = allTokens.stream()
                .filter(token -> Boolean.TRUE.equals(token.getRevoked()) || token.getExpiresAt().isBefore(now))
                .toList();

        if (!toDelete.isEmpty()) {
            refreshTokenRepo.deleteAll(toDelete);
        }
    }
}