package com.giapho.coffee_shop_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for improving application performance. Uses simple
 * in-memory caching for frequently accessed data.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS_CACHE = "products";
    public static final String CATEGORIES_CACHE = "categories";
    public static final String USERS_CACHE = "users";
    public static final String ROLES_CACHE = "roles";

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                PRODUCTS_CACHE,
                CATEGORIES_CACHE,
                USERS_CACHE,
                ROLES_CACHE
        );
    }
}
