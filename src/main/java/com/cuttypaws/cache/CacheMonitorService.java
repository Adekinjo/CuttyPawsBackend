package com.cuttypaws.cache;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class CacheMonitorService {

    private final CacheManager cacheManager;

    // Statistics tracking
    private final Map<String, AtomicLong> hitCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> missCount = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastAccess = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastUpdate = new ConcurrentHashMap<>();

    public CacheMonitorService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        initializeStatistics();
    }

    private void initializeStatistics() {
        // Initialize stats for all configured caches
        Arrays.asList(
                "products", "productById", "allCategories", "categoryById",
                "allSubCategories", "subCategoryById", "subCategorySearch",
                "subCategoriesByCategory", "categoryWithSubCategories", "categorySearch"
        ).forEach(cacheName -> {
            hitCount.put(cacheName, new AtomicLong(0));
            missCount.put(cacheName, new AtomicLong(0));
            lastAccess.put(cacheName, LocalDateTime.now());
            lastUpdate.put(cacheName, LocalDateTime.now());
        });
        log.info("📊 Initialized cache statistics tracking for {} caches", hitCount.size());
    }

    public void recordCacheHit(String cacheName) {
        hitCount.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        lastAccess.put(cacheName, LocalDateTime.now());
        log.debug("✅ Cache HIT: {}", cacheName);
    }

    public void recordCacheMiss(String cacheName) {
        missCount.computeIfAbsent(cacheName, k -> new AtomicLong(0)).incrementAndGet();
        lastAccess.put(cacheName, LocalDateTime.now());
        log.debug("❌ Cache MISS: {}", cacheName);
    }

    public void recordCacheUpdate(String cacheName) {
        lastUpdate.put(cacheName, LocalDateTime.now());
        lastAccess.put(cacheName, LocalDateTime.now());
        log.debug("🔄 Cache UPDATED: {}", cacheName);
    }

    public CacheStatistics getCacheStatistics(String cacheName) {
        long hits = hitCount.getOrDefault(cacheName, new AtomicLong(0)).get();
        long misses = missCount.getOrDefault(cacheName, new AtomicLong(0)).get();
        long total = hits + misses;

        double hitRate = total > 0 ? (double) hits / total * 100 : 0.0;
        String health = calculateHealthStatus(hitRate, total);

        return CacheStatistics.builder()
                .cacheName(cacheName)
                .hits(hits)
                .misses(misses)
                .hitRate(Math.round(hitRate * 100.0) / 100.0)
                .healthStatus(health)
                .totalAccess(total)
                .lastAccess(lastAccess.get(cacheName))
                .lastUpdate(lastUpdate.get(cacheName))
                .active(cacheManager.getCache(cacheName) != null)
                .build();
    }

    public Map<String, CacheStatistics> getAllCacheStatistics() {
        Map<String, CacheStatistics> stats = new LinkedHashMap<>();
        for (String cacheName : cacheManager.getCacheNames()) {
            stats.put(cacheName, getCacheStatistics(cacheName));
        }
        return stats;
    }

    public SystemCacheStats getSystemCacheStats() {
        Map<String, CacheStatistics> allStats = getAllCacheStatistics();

        long totalHits = allStats.values().stream().mapToLong(CacheStatistics::getHits).sum();
        long totalMisses = allStats.values().stream().mapToLong(CacheStatistics::getMisses).sum();
        long totalAccess = totalHits + totalMisses;
        double overallHitRate = totalAccess > 0 ? (double) totalHits / totalAccess * 100 : 0.0;

        Map<String, Long> healthDistribution = new HashMap<>();
        allStats.values().forEach(stat ->
                healthDistribution.merge(stat.getHealthStatus(), 1L, Long::sum)
        );

        return SystemCacheStats.builder()
                .totalCaches(allStats.size())
                .totalHits(totalHits)
                .totalMisses(totalMisses)
                .overallHitRate(Math.round(overallHitRate * 100.0) / 100.0)
                .healthDistribution(healthDistribution)
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String calculateHealthStatus(double hitRate, long totalAccess) {
        if (totalAccess == 0) return "IDLE";
        if (hitRate >= 80) return "EXCELLENT";
        if (totalAccess < 10) return "LOW_USAGE"; // New status for low usage
        if (hitRate >= 60) return "GOOD";
        if (hitRate >= 40) return "FAIR";
        if (hitRate >= 20) return "POOR";
        return "CRITICAL";
    }

    public void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            recordCacheUpdate(cacheName);
            log.info("🧹 Cleared cache: {}", cacheName);
        }
    }

    public void clearAllCaches() {
        cacheManager.getCacheNames().forEach(this::clearCache);
        log.info("🧹 Cleared all caches");
    }

    public void resetStatistics() {
        hitCount.clear();
        missCount.clear();
        initializeStatistics();
        log.info("🔄 Reset all cache statistics");
    }

    public void resetStatistics(String cacheName) {
        hitCount.put(cacheName, new AtomicLong(0));
        missCount.put(cacheName, new AtomicLong(0));
        lastAccess.put(cacheName, LocalDateTime.now());
        log.info("🔄 Reset statistics for cache: {}", cacheName);
    }

    // Scheduled health report every 5 minutes
    @Scheduled(fixedRate = 300000)
    public void generateHealthReport() {
        SystemCacheStats systemStats = getSystemCacheStats();
        log.info("📈 Cache Health Report - Overall Hit Rate: {}%, Caches: {}",
                systemStats.getOverallHitRate(), systemStats.getTotalCaches());
    }

    // Data classes
    @lombok.Builder
    @lombok.Data
    public static class CacheStatistics {
        private String cacheName;
        private long hits;
        private long misses;
        private double hitRate;
        private String healthStatus;
        private long totalAccess;
        private LocalDateTime lastAccess;
        private LocalDateTime lastUpdate;
        private boolean active;
    }

    @lombok.Builder
    @lombok.Data
    public static class SystemCacheStats {
        private int totalCaches;
        private long totalHits;
        private long totalMisses;
        private double overallHitRate;
        private Map<String, Long> healthDistribution;
        private LocalDateTime timestamp;
    }
    // Add this method to simulate initial cache activity
    @PostConstruct
    public void initializeCacheActivity() {
        log.info("🏁 Initializing cache activity monitoring...");

        // Schedule initial activity check after 1 minute
        CompletableFuture.delayedExecutor(1, TimeUnit.MINUTES).execute(() -> {
            SystemCacheStats stats = getSystemCacheStats();
            log.info("📊 Initial cache stats - Hit Rate: {}%, Total Accesses: {}",
                    stats.getOverallHitRate(),
                    stats.getTotalHits() + stats.getTotalMisses());
        });
    }
    @PostConstruct
    public void debugCacheConfiguration() {
        log.info("🔍 DEBUG: Checking cache configuration...");

        String[] cacheNames = cacheManager.getCacheNames().toArray(new String[0]);
        log.info("🔍 DEBUG: Found {} cache names: {}", cacheNames.length, Arrays.toString(cacheNames));

        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                log.info("✅ DEBUG: Cache '{}' is properly configured", cacheName);
            } else {
                log.error("❌ DEBUG: Cache '{}' is NULL - configuration issue!", cacheName);
            }
        }
    }
}

