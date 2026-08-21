package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("SELECT c FROM CartItem c JOIN FETCH c.product JOIN FETCH c.user WHERE c.user.id = :userId")
    List<CartItem> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM CartItem c JOIN FETCH c.product JOIN FETCH c.user WHERE c.user.id = :userId AND c.product.id = :productId")
    Optional<CartItem> findByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
