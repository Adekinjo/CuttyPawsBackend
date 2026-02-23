package com.cuttypaws.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisRateLimitService {

    private final StringRedisTemplate redis;

    // returns true if allowed, false if rate-limited
    public boolean allow(String key, int maxAttempts, Duration window) {
        String redisKey = "rl:" + key;

        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redis.expire(redisKey, window);
        }
        return count != null && count <= maxAttempts;
    }
}
