package com.cuttypaws.config;

import com.cuttypaws.cache.CacheMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheMonitoringAspect {

    private final CacheMonitorService cacheMonitorService;
    private final CacheManager cacheManager;

    // Fix: Remove parameter binding, use method signature instead
    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object monitorCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        String[] cacheNames = cacheable.value();
        if (cacheNames.length == 0) {
            return joinPoint.proceed();
        }

        String cacheName = cacheNames[0];
        Object key = generateCacheKey(joinPoint);

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("❌ Cache not found: {}", cacheName);
            return joinPoint.proceed();
        }

        // Check cache
        Cache.ValueWrapper cachedValue = cache.get(key);
        if (cachedValue != null) {
            cacheMonitorService.recordCacheHit(cacheName);
            log.info("🎯 Cache HIT - {} with key: {}", cacheName, key);
            return cachedValue.get();
        }

        // Cache miss - execute method
        cacheMonitorService.recordCacheMiss(cacheName);
        log.info("❌ Cache MISS - {} with key: {}", cacheName, key);

        Object result = joinPoint.proceed();

        // Cache the result if not null
        if (result != null) {
            try {
                cache.put(key, result);
                log.info("💾 Cached result in {} with key: {}", cacheName, key);
            } catch (Exception e) {
                log.error("Failed to cache result for {}: {}", cacheName, e.getMessage());
            }
        }

        return result;
    }

    @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public Object monitorCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);

        Object result = joinPoint.proceed();

        if (cacheEvict != null) {
            for (String cacheName : cacheEvict.value()) {
                cacheMonitorService.recordCacheUpdate(cacheName);
                log.info("🔄 Cache EVICT - {}", cacheName);
            }
        }

        return result;
    }

    private Object generateCacheKey(ProceedingJoinPoint joinPoint) {
        // Simple key generation - you can enhance this
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return "default";
        }

        // For methods with single ID parameter
        if (args.length == 1 && args[0] != null) {
            return args[0].toString();
        }

        // For multiple parameters, create a composite key
        StringBuilder keyBuilder = new StringBuilder();
        for (Object arg : args) {
            if (arg != null) {
                keyBuilder.append(arg.toString()).append(":");
            }
        }
        return keyBuilder.length() > 0 ? keyBuilder.toString() : "default";
    }
}