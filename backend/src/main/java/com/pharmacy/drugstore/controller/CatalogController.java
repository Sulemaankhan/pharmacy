package com.pharmacy.drugstore.controller;

import com.pharmacy.drugstore.entity.Category;
import com.pharmacy.drugstore.entity.Product;
import com.pharmacy.drugstore.repository.CategoryRepository;
import com.pharmacy.drugstore.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogController(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return categories.findAll();
    }

    @GetMapping("/products")
    public List<Product> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean deals,
            @RequestParam(required = false) Boolean featured) {
        if (q != null && !q.isBlank()) return products.search(q.trim());
        if (Boolean.TRUE.equals(deals)) return products.findByDealOfTheDayTrue();
        if (Boolean.TRUE.equals(featured)) return products.findByFeaturedTrue();
        if (categoryId != null) return products.findByCategoryId(categoryId);
        return products.findAll();
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable Long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
