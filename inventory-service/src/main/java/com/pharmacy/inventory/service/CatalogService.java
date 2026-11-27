package com.pharmacy.inventory.service;


import java.util.List;
import com.pharmacy.inventory.entity.Category;
import com.pharmacy.inventory.entity.Product;
import com.pharmacy.inventory.repository.CategoryRepository;
import com.pharmacy.inventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    public static final String PRODUCT_LIST_CACHE = "productList";
    public static final String CATEGORY_LIST_CACHE = "categoryList";
    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);
    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Cacheable(cacheNames = CATEGORY_LIST_CACHE, sync = true)
    @Transactional(readOnly = true)
    public List<Category> categories() {
        return List.copyOf(categories.findAll());
    }

    @Cacheable(cacheNames = PRODUCT_LIST_CACHE, key = "#query", sync = true)
    @Transactional(readOnly = true)
    public List<Product> products(ProductListQuery query) {
        List<Product> list = load(query);
        log.info("Product list loaded from database count={} categoryId={} q={} deals={} featured={} labPanel={}",
                list.size(), query.categoryId(), query.q(), query.deals(), query.featured(), query.labPanel());
        return List.copyOf(list);
    }

    private List<Product> load(ProductListQuery query) {
        if (query.labPanel() != null) {
            if ("ALL".equals(query.labPanel())) {
                return products.findByLabPanelIsNotNull();
            }
            return products.findByLabPanel(query.labPanel());
        }
        if (query.q() != null) {
            return products.search(query.q());
        }
        if (query.deals()) {
            return products.findByDealOfTheDayTrue();
        }
        if (query.featured()) {
            return products.findByFeaturedTrue();
        }
        if (query.categoryId() != null) {
            return products.findByCategoryId(query.categoryId());
        }
        return products.findAll();
    }
}
