package com.pharmacy.drugstore.repository;

import com.pharmacy.drugstore.entity.Shipment;
import com.pharmacy.drugstore.shipment.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @Query("SELECT s FROM Shipment s JOIN FETCH s.order o WHERE o.user.id = :userId ORDER BY s.createdAt DESC")
    List<Shipment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT s FROM Shipment s JOIN FETCH s.order o WHERE s.trackingNumber = :trackingNumber AND o.user.id = :userId")
    Optional<Shipment> findByTrackingNumberAndUserId(
            @Param("trackingNumber") String trackingNumber, @Param("userId") Long userId);

    @Query("SELECT s FROM Shipment s JOIN FETCH s.order o WHERE o.orderNumber = :orderNumber AND o.user.id = :userId")
    Optional<Shipment> findByOrderNumberAndUserId(
            @Param("orderNumber") String orderNumber, @Param("userId") Long userId);

    List<Shipment> findByStatusIn(Collection<ShipmentStatus> statuses);
}
