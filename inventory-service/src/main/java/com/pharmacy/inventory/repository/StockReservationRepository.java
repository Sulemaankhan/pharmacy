package com.pharmacy.inventory.repository;

import com.pharmacy.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    Optional<StockReservation> findByOrderNumber(String orderNumber);
}
