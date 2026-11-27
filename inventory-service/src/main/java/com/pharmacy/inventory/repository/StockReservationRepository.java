package com.pharmacy.inventory.repository;


import java.util.Optional;
import com.pharmacy.inventory.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    Optional<StockReservation> findByOrderNumber(String orderNumber);
}
