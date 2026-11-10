package com.pharmacy.drugstore.client;

import com.pharmacy.drugstore.entity.Category;
import com.pharmacy.drugstore.entity.Product;
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
    private final RestClient rest;

    public InventoryClient(@Value("${app.inventory-service.base-url:http://localhost:8088}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<Category> categories() {
        try {
            List<Category> list = rest.get()
                    .uri("/api/categories")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return list == null ? List.of() : list;
        } catch (Exception ex) {
            throw unavailable(ex, "categories");
        }
    }

    public List<Product> products(Long categoryId, String q, Boolean deals, Boolean featured, String labPanel) {
        try {
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
        } catch (Exception ex) {
            throw unavailable(ex, "products");
        }
    }

    public Product product(Long id) {
        try {
            Product product = rest.get().uri("/api/products/{id}", id).retrieve().body(Product.class);
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            }
            return product;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientResponseException ex) {
            throw new ResponseStatusException(HttpStatus.valueOf(ex.getStatusCode().value()), "Product not found");
        } catch (Exception ex) {
            throw unavailable(ex, "product " + id);
        }
    }

    private ResponseStatusException unavailable(Exception ex, String what) {
        log.error("Inventory catalog call failed {}", what, ex);
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Inventory service is unavailable. Start inventory-service on port 8088.");
    }
}
