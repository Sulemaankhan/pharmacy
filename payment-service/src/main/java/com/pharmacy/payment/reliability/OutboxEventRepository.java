package com.pharmacy.payment.reliability;


import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByStatusOrderByIdAsc(String status);
    Optional<OutboxEvent> findByEventId(String eventId);
}
