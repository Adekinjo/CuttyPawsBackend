package com.cuttypaws.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CacheAdminService {

    private final CacheManager cacheManager;

    public Map<String, Object> clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return Map.of(
                    "status", "error",
                    "message", "Cache not found: " + cacheName
            );
        }
        cache.clear();
        return Map.of(
                "status", "success",
                "message", "Cleared cache: " + cacheName,
                "cacheName", cacheName
        );
    }

    public Map<String, Object> clearAllCaches() {
        Collection<String> names = cacheManager.getCacheNames();
        for (String name : names) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        }
        return Map.of(
                "status", "success",
                "message", "Cleared all caches",
                "totalCaches", names.size()
        );
    }

//    public Map<String, Object> listCaches() {
//        Map<String, Object> out = new LinkedHashMap<>();
//        for (String name : cacheManager.getCacheNames()) {
//            out.put(name, Map.of("exists", cacheManager.getCache(name) != null));
//        }
//        return out;
//    }
}
