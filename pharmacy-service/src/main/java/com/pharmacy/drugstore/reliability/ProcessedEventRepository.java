package com.pharmacy.drugstore.reliability;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    boolean existsByConsumerNameAndEventId(String consumerName, String eventId);
}
