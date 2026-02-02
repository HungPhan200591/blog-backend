package com.hungpc.blog.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.concurrent.Callable;

/**
 * Logging wrapper for Spring Cache to track cache hits and misses
 */
@Slf4j
public class LoggingCacheWrapper implements Cache {

    private final Cache delegate;

    public LoggingCacheWrapper(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper result = delegate.get(key);

        if (result != null) {
            log.info("✅ Cache HIT - Cache: {}, Key: {}", getName(), key);
        } else {
            log.info("❌ Cache MISS - Cache: {}, Key: {}", getName(), key);
        }

        return result;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T result = delegate.get(key, type);

        if (result != null) {
            log.info("✅ Cache HIT - Cache: {}, Key: {}, Type: {}", getName(), key, type.getSimpleName());
        } else {
            log.info("❌ Cache MISS - Cache: {}, Key: {}, Type: {}", getName(), key, type.getSimpleName());
        }

        return result;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        // This method is called by @Cacheable when cache miss occurs
        try {
            T result = delegate.get(key, valueLoader);
            // If we reach here, it means cache was populated
            log.info("✅ Cache POPULATED - Cache: {}, Key: {}", getName(), key);
            return result;
        } catch (Exception e) {
            log.error("❌ Cache ERROR - Cache: {}, Key: {}, Error: {}", getName(), key, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
        log.info("💾 Cache PUT - Cache: {}, Key: {}", getName(), key);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper result = delegate.putIfAbsent(key, value);
        if (result == null) {
            log.info("💾 Cache PUT (new) - Cache: {}, Key: {}", getName(), key);
        } else {
            log.info("⏭️ Cache PUT (exists) - Cache: {}, Key: {}", getName(), key);
        }
        return result;
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
        log.info("🗑️ Cache EVICT (single) - Cache: {}, Key: {}", getName(), key);
    }

    @Override
    public void clear() {
        delegate.clear();
        log.info("🗑️ Cache CLEAR (all) - Cache: {}", getName());
    }
}
