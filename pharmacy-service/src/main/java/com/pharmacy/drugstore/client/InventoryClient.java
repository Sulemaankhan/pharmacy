package com.pharmacy.drugstore.client;

import com.pharmacy.drugstore.entity.Category;
import com.pharmacy.drugstore.entity.Product;
import com.pharmacy.drugstore.resilience.OutboundRest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Component
public class InventoryClient {
    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    private static final String UNAVAILABLE = "Inventory service is unavailable. Start inventory-service on port 8088.";
    private final RestClient rest;

    public InventoryClient(@Value("${app.inventory-service.base-url:http://localhost:8088}") String baseUrl) {
        this.rest = OutboundRest.client(baseUrl);
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "categoriesFallback")
    public List<Category> categories() {
        List<Category> list = rest.get()
                .uri("/api/categories")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return list == null ? List.of() : list;
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "productsFallback")
    public List<Product> products(Long categoryId, String q, Boolean deals, Boolean featured, String labPanel) {
        List<Product> list = rest.get()
                .uri(uri -> {
                    var builder = uri.path("/api/products");
                    if (categoryId != null) {
                        builder.queryParam("categoryId", categoryId);
                    }
                    if (q != null && !q.isBlank()) {
                        builder.queryParam("q", q);
                    }
                    if (deals != null) {
                        builder.queryParam("deals", deals);
                    }
                    if (featured != null) {
                        builder.queryParam("featured", featured);
                    }
                    if (labPanel != null && !labPanel.isBlank()) {
                        builder.queryParam("labPanel", labPanel);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return list == null ? List.of() : list;
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "productFallback")
    public Product product(Long id) {
        try {
            Product product = rest.get().uri("/api/products/{id}", id).retrieve().body(Product.class);
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }
            return product;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(HttpStatus.valueOf(ex.getStatusCode().value()), "Product not found");
            }
            throw ex;
        }
    }

    private List<Category> categoriesFallback(Throwable ex) {
        throw unavailable(ex, "categories");
    }

    private List<Product> productsFallback(
            Long categoryId, String q, Boolean deals, Boolean featured, String labPanel, Throwable ex) {
        throw unavailable(ex, "products");
    }

    private Product productFallback(Long id, Throwable ex) {
        if (ex instanceof ResponseStatusException status) {
            throw status;
        }
        throw unavailable(ex, "product " + id);
    }

    private ResponseStatusException unavailable(Throwable ex, String what) {
        if (ex instanceof ResponseStatusException status) {
            return status;
        }
        log.warn("Inventory catalog call failed {} reason={}", what, OutboundRest.reason(ex));
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }
}
