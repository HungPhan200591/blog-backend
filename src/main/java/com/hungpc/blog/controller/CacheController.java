package com.hungpc.blog.controller;

import com.hungpc.blog.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for cache management operations
 */
@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheController {

    private final CacheManager cacheManager;

    /**
     * Clear all application caches
     * 
     * @return Response with cleared cache count
     */
    @PostMapping("/clear")
    public ResponseEntity<BaseResponse<Map<String, Object>>> clearAllCaches() {
        log.info("🗑️  Clearing all caches...");
        long startTime = System.currentTimeMillis();

        int clearedCount = 0;

        // Clear all caches
        for (String cacheName : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                clearedCount++;
                log.debug("Cleared cache: {}", cacheName);
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("✅ Cleared {} caches in {}ms", clearedCount, duration);

        Map<String, Object> data = new HashMap<>();
        data.put("clearedCaches", clearedCount);
        data.put("cacheNames", cacheManager.getCacheNames());
        data.put("durationMs", duration);

        String message = String.format("Cleared %d caches successfully in %dms", clearedCount, duration);
        return ResponseEntity.ok(BaseResponse.success(message, data));
    }
}
