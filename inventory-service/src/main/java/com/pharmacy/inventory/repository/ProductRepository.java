package com.pharmacy.inventory.repository;

import com.pharmacy.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByDealOfTheDayTrue();
    List<Product> findByFeaturedTrue();
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByLabPanelIsNotNull();
    List<Product> findByLabPanel(String labPanel);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Product> search(@Param("q") String q);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> lockAllById(@Param("ids") Collection<Long> ids);
}
