package com.pharmacy.inventory.repository;

import com.pharmacy.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByDealOfTheDayTrue();
    List<Product> findByFeaturedTrue();
    List<Product> findByCategoryId(Long categoryId);
    List<Product> findByLabPanelIsNotNull();
    List<Product> findByLabPanel(String labPanel);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Product> search(@Param("q") String q);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty", nativeQuery = true)
    int decrementIfAvailable(@Param("id") Long id, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE products SET stock = stock + :qty WHERE id = :id", nativeQuery = true)
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);
}
