package com.pharmacy.inventory.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.pharmacy.inventory.service.CatalogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import java.time.Duration;

@Configuration
@EnableCaching(order = Ordered.HIGHEST_PRECEDENCE)
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(
            @Value("${app.cache.products.ttl-seconds:60}") long ttlSeconds,
            @Value("${app.cache.products.max-size:200}") long maxSize) {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CatalogService.PRODUCT_LIST_CACHE, CatalogService.CATEGORY_LIST_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxSize));
        return manager;
    }
}
