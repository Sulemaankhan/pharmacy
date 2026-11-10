package com.pharmacy.inventory.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    @Column(precision = 10, scale = 2)
    private BigDecimal compareAtPrice;
    private String imageUrl;
    private String brand;
    private Integer stock = 0;
    private Double rating = 4.5;
    private Integer reviewCount = 0;
    private boolean dealOfTheDay = false;
    private Instant dealEndsAt;
    private boolean featured = false;
    @Column(length = 32)
    private String labPanel;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getCompareAtPrice() { return compareAtPrice; }
    public String getImageUrl() { return imageUrl; }
    public String getBrand() { return brand; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Double getRating() { return rating; }
    public Integer getReviewCount() { return reviewCount; }
    public boolean isDealOfTheDay() { return dealOfTheDay; }
    public Instant getDealEndsAt() { return dealEndsAt; }
    public boolean isFeatured() { return featured; }
    public String getLabPanel() { return labPanel; }
    public Category getCategory() { return category; }
}
