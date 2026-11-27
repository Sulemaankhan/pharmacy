package com.pharmacy.shipment.repository;


import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.pharmacy.shipment.entity.Shipment;
import com.pharmacy.shipment.shipment.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    Optional<Shipment> findByOrderId(Long orderId);
    Optional<Shipment> findByOrderNumberAndUserId(String orderNumber, Long userId);
    List<Shipment> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Shipment> findByStatusIn(Collection<ShipmentStatus> statuses);

    @Query("select s.id from Shipment s where s.status in :statuses")
    List<Long> findIdsByStatusIn(@Param("statuses") Collection<ShipmentStatus> statuses);
}
