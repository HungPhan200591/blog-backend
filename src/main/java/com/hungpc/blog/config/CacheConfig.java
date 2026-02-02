package com.hungpc.blog.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hungpc.blog.constant.CacheConstants;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // Create Caffeine cache manager
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager(
                CacheConstants.POSTS,
                CacheConstants.POST_BY_SLUG,
                CacheConstants.RELATED_POSTS,
                CacheConstants.FEATURED_POSTS,
                CacheConstants.LATEST_POSTS,
                CacheConstants.CATEGORIES,
                CacheConstants.TAGS,
                CacheConstants.SERIES);
        caffeineCacheManager.setCaffeine(caffeineCacheBuilder());

        // Wrap all caches with logging wrapper for cache hit/miss tracking
        SimpleCacheManager wrappedCacheManager = new SimpleCacheManager();
        List<Cache> wrappedCaches = caffeineCacheManager.getCacheNames().stream()
                .map(caffeineCacheManager::getCache)
                .map(LoggingCacheWrapper::new)
                .collect(Collectors.toList());
        wrappedCacheManager.setCaches(wrappedCaches);
        wrappedCacheManager.initializeCaches();

        log.info("🚀 Cache Manager initialized with logging for caches: {}",
                caffeineCacheManager.getCacheNames());

        return wrappedCacheManager;
    }

    @Bean
    public Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(2000) // Prevent memory overflow
                .expireAfterWrite(24, TimeUnit.HOURS) // Safety net for stale data
                .recordStats() // Enable monitoring
                .removalListener((Object key, Object value, RemovalCause cause) -> {
                    // Log cache evictions
                    log.info("🗑️ Cache EVICTED - Key: {}, Cause: {}", key, cause);
                })
                .evictionListener((Object key, Object value, RemovalCause cause) -> {
                    // Log cache evictions due to size/time
                    log.warn("⚠️ Cache EVICTION (auto) - Key: {}, Cause: {}", key, cause);
                });
    }
}
