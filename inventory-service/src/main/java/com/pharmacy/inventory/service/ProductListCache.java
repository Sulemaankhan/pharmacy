package com.pharmacy.inventory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ProductListCache {
    private static final Logger log = LoggerFactory.getLogger(ProductListCache.class);
    private final CacheManager cacheManager;

    public ProductListCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            clear();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                clear();
            }
        });
    }

    private void clear() {
        Cache cache = cacheManager.getCache(CatalogService.PRODUCT_LIST_CACHE);
        if (cache == null) {
            return;
        }
        cache.clear();
        log.info("Product list cache cleared");
    }
}
