package com.cuttypaws.controller;

import com.cuttypaws.cache.CacheMonitorService;
import com.cuttypaws.cache.CacheToggleService;
import com.cuttypaws.config.CacheAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheAdminService cacheAdminService;
    private final CacheToggleService cacheToggleService;

    // ✅ ADD THESE
    private final CacheMonitorService cacheMonitorService;
    private final RedisConnectionFactory redisConnectionFactory;

    // ✅ NEW: DASHBOARD
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> dashboard() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("system", cacheMonitorService.getSystemCacheStats());
        body.put("caches", cacheMonitorService.getAllCacheStatistics());
        body.put("redis", redisInfo());
        return ResponseEntity.ok(withTimestamp(body));
    }

    @PostMapping("/clear/{cacheName}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable String cacheName) {
        Map<String, Object> result = cacheAdminService.clearCache(cacheName);
        return ResponseEntity.ok(withTimestamp(result));
    }

    @PostMapping("/clear-all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        Map<String, Object> result = cacheAdminService.clearAllCaches();
        return ResponseEntity.ok(withTimestamp(result));
    }

    @GetMapping("/toggle/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> cacheToggleStatus() {
        return ResponseEntity.ok(withTimestamp(Map.of(
                "status", "success",
                "caching", cacheToggleService.isEnabled() ? "ENABLED" : "DISABLED"
        )));
    }

    @PostMapping("/toggle/disable")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> disableCaching() {
        cacheToggleService.disable();
        return ResponseEntity.ok(withTimestamp(Map.of(
                "status", "success",
                "caching", "DISABLED"
        )));
    }

    @PostMapping("/toggle/enable")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> enableCaching() {
        cacheToggleService.enable();
        return ResponseEntity.ok(withTimestamp(Map.of(
                "status", "success",
                "caching", "ENABLED"
        )));
    }
    @PostMapping("/stats/reset")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        cacheMonitorService.resetStatistics();
        return ResponseEntity.ok(withTimestamp(Map.of(
                "status", "success",
                "message", "Cache statistics reset successfully"
        )));
    }

    // ✅ NEW: generate activity endpoint (matches frontend)
    @PostMapping("/generate-activity")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> generateActivity() {

        // Simulate hits/misses for all caches
        cacheMonitorService.getAllCacheStatistics().keySet().forEach(cacheName -> {
            cacheMonitorService.recordCacheHit(cacheName);
            cacheMonitorService.recordCacheHit(cacheName);
            cacheMonitorService.recordCacheMiss(cacheName);
        });

        return ResponseEntity.ok(withTimestamp(Map.of(
                "status", "success",
                "message", "Cache activity generated successfully"
        )));
    }


    private Map<String, Object> redisInfo() {
        try (var conn = redisConnectionFactory.getConnection()) {
            String ping = conn.ping();
            return Map.of(
                    "status", "CONNECTED",
                    "ping", ping
            );
        } catch (Exception e) {
            return Map.of(
                    "status", "DISCONNECTED",
                    "error", e.getMessage()
            );
        }
    }

    private Map<String, Object> withTimestamp(Map<String, Object> data) {
        Map<String, Object> out = new LinkedHashMap<>(data);
        out.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        return out;
    }
}
