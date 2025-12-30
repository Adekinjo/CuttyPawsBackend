package com.cuttypaws.controller;

import com.cuttypaws.cache.CacheMonitorService;
import com.cuttypaws.response.ProductResponse;
import com.cuttypaws.service.interf.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheMonitorService cacheMonitorService;
    private final RedisConnectionFactory redisConnectionFactory;
    private final CacheManager cacheManager;
    private final ProductService productService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getCacheDashboard() {
        log.info("📊 Generating cache dashboard");

        Map<String, Object> response = new HashMap<>();

        // System statistics
        var systemStats = cacheMonitorService.getSystemCacheStats();
        response.put("system", systemStats);

        // Detailed cache statistics
        response.put("caches", cacheMonitorService.getAllCacheStatistics());

        // Redis info
        response.put("redis", getRedisInfo());

        // Timestamp
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        log.info("✅ Cache dashboard generated with {} caches", systemStats.getTotalCaches());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clear/{cacheName}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable String cacheName) {
        log.info("🧹 Clearing cache: {}", cacheName);

        Map<String, Object> response = new HashMap<>();
        try {
            cacheMonitorService.clearCache(cacheName);
            response.put("status", "success");
            response.put("message", "Cache '" + cacheName + "' cleared successfully");
            response.put("cacheName", cacheName);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to clear cache: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/clear-all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        log.info("🧹 Clearing all caches");

        cacheMonitorService.clearAllCaches();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All caches cleared successfully");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/stats/reset")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        log.info("🔄 Resetting cache statistics");

        cacheMonitorService.resetStatistics();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Cache statistics reset successfully");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        Map<String, Object> health = new HashMap<>();

        // System health
        var systemStats = cacheMonitorService.getSystemCacheStats();
        health.put("system", Map.of(
                "overallHitRate", systemStats.getOverallHitRate(),
                "totalCaches", systemStats.getTotalCaches(),
                "healthDistribution", systemStats.getHealthDistribution(),
                "status", systemStats.getOverallHitRate() > 50 ? "HEALTHY" : "DEGRADED"
        ));

        // Redis health
        health.put("redis", getRedisInfo());

        health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.ok(health);
    }

    @PostMapping("/generate-activity")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> generateCacheActivity() {
        log.info("🎯 Generating cache activity");

        Map<String, Object> response = new HashMap<>();
        try {
            // Simulate some cache activity
            cacheMonitorService.getAllCacheStatistics().keySet().forEach(cacheName -> {
                cacheMonitorService.recordCacheHit(cacheName);
                cacheMonitorService.recordCacheHit(cacheName);
                cacheMonitorService.recordCacheMiss(cacheName);
            });

            response.put("status", "success");
            response.put("message", "Cache activity generated successfully");
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to generate cache activity: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    private Map<String, Object> getRedisInfo() {
        Map<String, Object> redisInfo = new HashMap<>();
        try {
            var connection = redisConnectionFactory.getConnection();
            String ping = connection.ping();
            connection.close();

            redisInfo.put("status", "CONNECTED");
            redisInfo.put("ping", ping);
            redisInfo.put("message", "Redis is operating normally");
        } catch (Exception e) {
            redisInfo.put("status", "DISCONNECTED");
            redisInfo.put("error", e.getMessage());
            redisInfo.put("message", "Redis connection failed");
        }
        return redisInfo;
    }

    @PostMapping("/test-cache/{cacheName}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> testCache(@PathVariable String cacheName) {
        log.info("🧪 Testing cache: {}", cacheName);

        Map<String, Object> response = new HashMap<>();
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                response.put("status", "error");
                response.put("message", "Cache not found: " + cacheName);
                return ResponseEntity.badRequest().body(response);
            }

            // Test cache operations
            String testKey = "test-key-" + System.currentTimeMillis();
            String testValue = "test-value-" + System.currentTimeMillis();

            // Put value
            cache.put(testKey, testValue);

            // Get value
            Cache.ValueWrapper wrapper = cache.get(testKey);
            if (wrapper != null && testValue.equals(wrapper.get())) {
                response.put("status", "success");
                response.put("message", "Cache test successful for: " + cacheName);
                response.put("testKey", testKey);
                response.put("testValue", testValue);
            } else {
                response.put("status", "error");
                response.put("message", "Cache test failed for: " + cacheName);
            }

            // Clean up
            cache.evict(testKey);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Cache test error: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
    @GetMapping("/test-cache-operations")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> testCacheOperations() {
        log.info("🧪 Testing cache operations...");

        Map<String, Object> response = new HashMap<>();
        List<String> results = new ArrayList<>();

        try {
            // Test 1: Check if products cache works
            Cache productsCache = cacheManager.getCache("products");
            if (productsCache != null) {
                String testKey = "test-operation";
                String testValue = "test-value-" + System.currentTimeMillis();

                // Put value
                productsCache.put(testKey, testValue);
                results.add("✅ PUT operation successful for 'products' cache");

                // Get value
                Cache.ValueWrapper wrapper = productsCache.get(testKey);
                if (wrapper != null && testValue.equals(wrapper.get())) {
                    results.add("✅ GET operation successful for 'products' cache");
                } else {
                    results.add("❌ GET operation failed for 'products' cache");
                }

                // Clean up
                productsCache.evict(testKey);
            } else {
                results.add("❌ 'products' cache not found");
            }

            // Test 2: Manually trigger cacheable method
            try {
                // This should trigger the aspect if caching is working
                ProductResponse allProducts = productService.getAllProduct();
                results.add("✅ ProductService.getAllProduct() executed - check logs for cache behavior");
            } catch (Exception e) {
                results.add("❌ ProductService.getAllProduct() failed: " + e.getMessage());
            }

            response.put("status", "success");
            response.put("results", results);
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Test failed: " + e.getMessage());
            response.put("results", results);
        }

        return ResponseEntity.ok(response);
    }
}