package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.payment WHERE o.user = :user ORDER BY o.createdAt DESC")
    List<CustomerOrder> findByUserOrderByCreatedAtDesc(@Param("user") User user);

    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.payment WHERE o.orderNumber = :orderNumber AND o.user = :user")
    Optional<CustomerOrder> findByOrderNumberAndUser(@Param("orderNumber") String orderNumber, @Param("user") User user);
}
