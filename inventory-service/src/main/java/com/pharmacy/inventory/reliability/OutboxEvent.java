package com.pharmacy.inventory.reliability;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_outbox_events")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String eventId;
    @Column(nullable = false, length = 64)
    private String eventType;
    @Column(nullable = false)
    private String aggregateId;
    @Column(nullable = false)
    private String destinationTopic;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    @Column(nullable = false, length = 16)
    private String status = "PENDING";
    private int attempts;
    @Column(length = 500)
    private String lastError;
    private Instant createdAt = Instant.now();
    private Instant publishedAt;

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getAggregateId() { return aggregateId; }
    public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
    public String getDestinationTopic() { return destinationTopic; }
    public void setDestinationTopic(String destinationTopic) { this.destinationTopic = destinationTopic; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
}
