package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    @Query("SELECT w FROM WishlistItem w JOIN FETCH w.product JOIN FETCH w.user WHERE w.user.id = :userId")
    List<WishlistItem> findByUserId(@Param("userId") Long userId);

    @Query("SELECT w FROM WishlistItem w WHERE w.user.id = :userId AND w.product.id = :productId")
    Optional<WishlistItem> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    long countByUser_Id(Long userId);
}
