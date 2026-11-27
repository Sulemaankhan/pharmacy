package com.pharmacy.inventory.controller;


import java.time.Duration;
import java.util.List;
import com.pharmacy.inventory.entity.Category;
import com.pharmacy.inventory.entity.Product;
import com.pharmacy.inventory.repository.ProductRepository;
import com.pharmacy.inventory.service.CatalogService;
import com.pharmacy.inventory.service.ProductListQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final ProductRepository products;
    private final CatalogService catalog;
    private final Duration cacheTtl;

    public CatalogController(
            ProductRepository products,
            CatalogService catalog,
            @Value("${app.cache.products.ttl-seconds:60}") long ttlSeconds) {
        this.products = products;
        this.catalog = catalog;
        this.cacheTtl = Duration.ofSeconds(Math.max(ttlSeconds, 1));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> categories() {
        return cached(catalog.categories());
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean deals,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String labPanel) {
        return cached(catalog.products(ProductListQuery.from(categoryId, q, deals, featured, labPanel)));
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable Long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private <T> ResponseEntity<T> cached(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(cacheTtl).cachePublic().mustRevalidate())
                .body(body);
    }
}
