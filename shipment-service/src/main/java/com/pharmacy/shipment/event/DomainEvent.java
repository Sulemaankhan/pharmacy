package com.pharmacy.shipment.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DomainEvent {
    private String eventId;
    private String eventType;
    private String orderNumber;
    private String correlationId;
    private String occurredAt = Instant.now().toString();
    private Map<String, Object> payload = new LinkedHashMap<>();

    public DomainEvent() {}

    public static DomainEvent of(String eventType, String orderNumber, String correlationId, Map<String, Object> payload) {
        DomainEvent event = new DomainEvent();
        event.eventId = UUID.randomUUID().toString();
        event.eventType = eventType;
        event.orderNumber = orderNumber;
        event.correlationId = correlationId;
        event.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        event.occurredAt = Instant.now().toString();
        return event;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
}
