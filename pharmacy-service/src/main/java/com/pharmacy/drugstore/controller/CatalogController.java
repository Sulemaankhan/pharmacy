package com.pharmacy.drugstore.controller;

import com.pharmacy.drugstore.client.InventoryClient;
import com.pharmacy.drugstore.entity.Category;
import com.pharmacy.drugstore.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);
    private final InventoryClient inventory;

    public CatalogController(InventoryClient inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        List<Category> list = inventory.categories();
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
        List<Product> list = inventory.products(categoryId, q, deals, featured, labPanel);
        log.info("Catalog products count={} categoryId={} q={} deals={} featured={} labPanel={}",
                list.size(), categoryId, q, deals, featured, labPanel);
        return list;
    }

    @GetMapping("/products/{id}")
    public Product product(@PathVariable Long id) {
        log.info("Catalog product id={}", id);
        return inventory.product(id);
    }
}
