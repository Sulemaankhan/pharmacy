package com.pharmacy.drugstore.reliability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "pharmacy_processed_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_pharmacy_processed_event", columnNames = {"consumer_name", "event_id"})
)
public class ProcessedEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "consumer_name", nullable = false, length = 80)
    private String consumerName;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(length = 64)
    private String eventType;

    private String aggregateId;
    private Instant processedAt = Instant.now();

    public void setConsumerName(String consumerName) { this.consumerName = consumerName; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
}
