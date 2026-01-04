package com.northgod.server.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 书籍缓存：较大容量，较长时间
        cacheManager.registerCustomCache("books",
                Caffeine.newBuilder()
                        .maximumSize(1000)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .expireAfterAccess(15, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // 交易记录缓存：中等容量，较短时间（交易数据变化频繁）
        cacheManager.registerCustomCache("transactions",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        // 供应商缓存：较小容量，较长时间
        cacheManager.registerCustomCache("suppliers",
                Caffeine.newBuilder()
                        .maximumSize(200)
                        .expireAfterAccess(30, TimeUnit.MINUTES)
                        .expireAfterWrite(60, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
        
        // 统计结果缓存：较小容量，较短时间（统计数据需要相对实时）
        cacheManager.registerCustomCache("statistics",
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .recordStats()
                        .build());
        
        // 搜索结果缓存：小容量，短时间
        cacheManager.registerCustomCache("searchResults",
                Caffeine.newBuilder()
                        .maximumSize(50)
                        .expireAfterWrite(1, TimeUnit.MINUTES)
                        .recordStats()
                        .build());

        return cacheManager;
    }
}