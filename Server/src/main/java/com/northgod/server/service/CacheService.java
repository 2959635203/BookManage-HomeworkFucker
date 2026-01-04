package com.northgod.server.service;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Cacheable(value = "books", key = "#id", unless = "#result == null")
    public String getBookCache(Long id, String data) {
        logger.debug("缓存未命中，加载书籍数据: {}", id);
        return data;
    }

    @CachePut(value = "books", key = "#id")
    public String updateBookCache(Long id, String data) {
        logger.debug("更新书籍缓存: {}", id);
        return data;
    }

    public void evictBookCache(Long id) {
        logger.debug("清除书籍缓存: {}", id);
        // 手动清除缓存，避免SpEL表达式参数名称问题
        var cache = cacheManager.getCache("books");
        if (cache != null && id != null) {
            cache.evict(id);
        }
    }

    @CacheEvict(value = "books", allEntries = true)
    public void evictAllBookCache() {
        logger.debug("清除所有书籍缓存");
    }

    @CacheEvict(value = {"books", "transactions", "suppliers", "statistics", "searchResults"}, allEntries = true)
    public void evictAllCaches() {
        logger.info("清除所有缓存");
    }

    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        Objects.requireNonNull(cacheManager.getCacheNames()).forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache caffeineCache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
                        caffeineCache.getNativeCache();
                CacheStats cacheStats = nativeCache.stats();

                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("estimatedSize", nativeCache.estimatedSize());
                cacheInfo.put("hitRate", cacheStats.hitRate());
                cacheInfo.put("missRate", cacheStats.missRate());
                cacheInfo.put("loadSuccessCount", cacheStats.loadSuccessCount());
                cacheInfo.put("loadFailureCount", cacheStats.loadFailureCount());
                cacheInfo.put("totalLoadTime", cacheStats.totalLoadTime());
                cacheInfo.put("evictionCount", cacheStats.evictionCount());

                stats.put(cacheName, cacheInfo);
            }
        });

        return stats;
    }

    public Map<String, Object> getCacheContents(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache instanceof CaffeineCache caffeineCache) {
            ConcurrentMap<Object, Object> cacheMap = caffeineCache.getNativeCache().asMap();
            Map<String, Object> contents = new HashMap<>();

            cacheMap.forEach((key, value) -> {
                contents.put(key.toString(), value.toString());
            });

            return contents;
        }
        return Map.of();
    }
}