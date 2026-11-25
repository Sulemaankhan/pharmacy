package com.pharmacy.inventory.service;

import com.pharmacy.inventory.config.CacheConfig;
import com.pharmacy.inventory.repository.CategoryRepository;
import com.pharmacy.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CatalogServiceCacheTest.Config.class)
class CatalogServiceCacheTest {
    @Autowired
    private CatalogService catalog;
    @Autowired
    private ProductRepository products;
    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        reset(products);
        cacheManager.getCache(CatalogService.PRODUCT_LIST_CACHE).clear();
    }

    @Test
    void repeatedProductListSkipsTheDatabase() {
        when(products.findAll()).thenReturn(List.of());

        assertThat(catalog.products(ProductListQuery.from(null, null, null, null, null))).isEmpty();
        assertThat(catalog.products(ProductListQuery.from(null, "  ", false, null, " "))).isEmpty();

        verify(products, times(1)).findAll();
    }

    @Test
    void sameSearchWithDifferentCaseSharesOneCacheEntry() {
        when(products.search("aspirin")).thenReturn(List.of());

        catalog.products(ProductListQuery.from(null, "Aspirin", null, null, null));
        catalog.products(ProductListQuery.from(null, " aspirin ", null, null, null));

        verify(products, times(1)).search("aspirin");
    }

    @Test
    void differentCategoryLoadsAgain() {
        when(products.findAll()).thenReturn(List.of());
        when(products.findByCategoryId(4L)).thenReturn(List.of());

        catalog.products(ProductListQuery.from(null, null, null, null, null));
        catalog.products(ProductListQuery.from(4L, null, null, null, null));

        verify(products, times(1)).findAll();
        verify(products, times(1)).findByCategoryId(4L);
    }

    @Configuration
    @Import({CatalogService.class, CacheConfig.class})
    static class Config {
        @Bean
        ProductRepository productRepository() {
            return mock(ProductRepository.class);
        }

        @Bean
        CategoryRepository categoryRepository() {
            return mock(CategoryRepository.class);
        }
    }
}
