package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.payment LEFT JOIN FETCH o.shipment WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.payment LEFT JOIN FETCH o.shipment WHERE o.orderNumber = :orderNumber AND o.user.id = :userId")
    Optional<CustomerOrder> findByOrderNumberAndUserId(@Param("orderNumber") String orderNumber, @Param("userId") Long userId);

    @Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    Optional<CustomerOrder> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);
}
