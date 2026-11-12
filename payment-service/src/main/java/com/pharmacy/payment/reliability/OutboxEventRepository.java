package com.pharmacy.payment.reliability;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByStatusOrderByIdAsc(String status);
    Optional<OutboxEvent> findByEventId(String eventId);
}
