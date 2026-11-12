package com.pharmacy.shipment.reliability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.shipment.event.DomainEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class OutboxService {
    private final OutboxEventRepository outbox;
    private final ObjectMapper mapper;

    public OutboxService(OutboxEventRepository outbox, ObjectMapper mapper) {
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @Transactional
    public void enqueue(String topic, String orderNumber, DomainEvent event) {
        enqueue(topic, orderNumber, event.getEventType(), event.getEventId(), event);
    }

    @Transactional
    public void enqueue(String topic, String orderNumber, String eventType, Object payload) {
        enqueue(topic, orderNumber, eventType, UUID.randomUUID().toString(), payload);
    }

    @Transactional
    public void enqueue(String topic, String orderNumber, String eventType, String eventId, Object payload) {
        try {
            OutboxEvent row = new OutboxEvent();
            row.setEventId(eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId);
            row.setEventType(eventType);
            row.setAggregateId(orderNumber);
            row.setDestinationTopic(topic);
            row.setPayload(mapper.writeValueAsString(payload));
            row.setStatus("PENDING");
            outbox.save(row);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize shipment outbox payload", ex);
        }
    }
}
