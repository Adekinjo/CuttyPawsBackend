package com.cuttypaws.security;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitService {

    private final Map<String, RateLimitRecord> rateLimitMap = new ConcurrentHashMap<>();

    public boolean isRateLimited(String identifier, String type) {
        String key = identifier + ":" + type;
        RateLimitRecord record = rateLimitMap.get(key);

        if (record == null) {
            record = new RateLimitRecord(type);
            rateLimitMap.put(key, record);
            return false;
        }

        return record.isRateLimited();
    }

    public void recordAttempt(String identifier, String type) {
        String key = identifier + ":" + type;
        RateLimitRecord record = rateLimitMap.computeIfAbsent(key, k -> new RateLimitRecord(type));
        record.incrementAttempt();
    }

    // For password reset - 3 attempts per 15 minutes
    public boolean isPasswordResetLimited(String email) {
        return isRateLimited(email, "PASSWORD_RESET");
    }

    // For login - 5 attempts per 30 minutes
    public boolean isLoginLimited(String email) {
        return isRateLimited(email, "LOGIN");
    }

    // For registration - 3 attempts per hour
    public boolean isRegistrationLimited(String ip) {
        return isRateLimited(ip, "REGISTRATION");
    }

    private static class RateLimitRecord {
        private int attemptCount;
        private LocalDateTime firstAttempt;
        private LocalDateTime lastAttempt;
        private final String type;

        public RateLimitRecord(String type) {
            this.type = type;
            this.attemptCount = 0;
            this.firstAttempt = LocalDateTime.now();
            this.lastAttempt = LocalDateTime.now();
        }

        public void incrementAttempt() {
            this.attemptCount++;
            this.lastAttempt = LocalDateTime.now();
        }

        public boolean isRateLimited() {
            int maxAttempts = getMaxAttempts();
            int windowMinutes = getWindowMinutes();

            // Reset if window has passed
            if (firstAttempt.plusMinutes(windowMinutes).isBefore(LocalDateTime.now())) {
                this.attemptCount = 0;
                this.firstAttempt = LocalDateTime.now();
                return false;
            }

            return attemptCount >= maxAttempts;
        }

        private int getMaxAttempts() {
            return switch (type) {
                case "PASSWORD_RESET" -> 3;
                case "LOGIN" -> 5;
                case "REGISTRATION" -> 3;
                default -> 10;
            };
        }

        private int getWindowMinutes() {
            return switch (type) {
                case "PASSWORD_RESET" -> 15;
                case "LOGIN" -> 30;
                case "REGISTRATION" -> 60;
                default -> 60;
            };
        }
    }
}
