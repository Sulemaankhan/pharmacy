package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.CartItem;
import com.pharmacy.drugstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByUser(User user);
    Optional<CartItem> findByUserAndProductId(User user, Long productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartItem c WHERE c.user = :user")
    int deleteByUser(@Param("user") User user);
}
