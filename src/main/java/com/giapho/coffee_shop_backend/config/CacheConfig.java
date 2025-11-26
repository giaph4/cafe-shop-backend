package com.giapho.coffee_shop_backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for improving application performance.
 * Uses Caffeine for high-performance in-memory caching.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS_CACHE = "products";
    public static final String CATEGORIES_CACHE = "categories";
    public static final String USERS_CACHE = "users";
    public static final String ROLES_CACHE = "roles";
    public static final String ADMIN_DASHBOARD_CACHE = "adminDashboard";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                PRODUCTS_CACHE,
                CATEGORIES_CACHE,
                USERS_CACHE,
                ROLES_CACHE,
                ADMIN_DASHBOARD_CACHE
        );
        
        // Configure Caffeine cache with eviction policy
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats());
                
        return cacheManager;
    }
}
