package com.cuttypaws.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class SecurityHitCounter {

    private final StringRedisTemplate redis;

    /**
     * Increments a Redis counter and sets TTL on the first hit.
     * Returns the current count after increment.
     */
    public long incrementWithTtl(String key, Duration ttl) {
        String redisKey = "sec:hits:" + key;

        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redis.expire(redisKey, ttl);
        }
        return count == null ? 0 : count;
    }
}
