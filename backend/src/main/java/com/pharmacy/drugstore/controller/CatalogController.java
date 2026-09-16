package com.pharmacy.drugstore.controller;

import com.pharmacy.drugstore.entity.Category;
import com.pharmacy.drugstore.entity.Product;
import com.pharmacy.drugstore.repository.CategoryRepository;
import com.pharmacy.drugstore.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);
    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogController(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        List<Category> list = categories.findAll();
        log.info("Catalog categories count={}", list.size());
        return list;
    }

    @GetMapping("/products")
    public List<Product> products(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean deals,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String labPanel) {
        List<Product> list;
        if (labPanel != null && !labPanel.isBlank()) {
            if ("ALL".equalsIgnoreCase(labPanel.trim())) list = products.findByLabPanelIsNotNull();
            else list = products.findByLabPanel(labPanel.trim().toUpperCase());
        } else if (q != null && !q.isBlank()) list = products.search(q.trim());
        else if (Boolean.TRUE.equals(deals)) list = products.findByDealOfTheDayTrue();
        else if (Boolean.TRUE.equals(featured)) list = products.findByFeaturedTrue();
        else if (categoryId != null) list = products.findByCategoryId(categoryId);
        else list = products.findAll();
        log.info("Catalog products count={} categoryId={} q={} deals={} featured={} labPanel={}",
                list.size(), categoryId, q, deals, featured, labPanel);
        return list;
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable Long id) {
        log.info("Catalog product id={}", id);
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}
