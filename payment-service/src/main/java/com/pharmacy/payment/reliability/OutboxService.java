package com.pharmacy.payment.reliability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.payment.event.DomainEvent;
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
    public void enqueue(String topic, String orderNumber, String eventType, String eventId, Object payload) {
        try {
            String id = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
            var existing = outbox.findByEventId(id);
            if (existing.isPresent()) {
                OutboxEvent row = existing.get();
                row.setStatus("PENDING");
                row.setAttempts(0);
                row.setLastError(null);
                outbox.save(row);
                return;
            }
            OutboxEvent row = new OutboxEvent();
            row.setEventId(id);
            row.setEventType(eventType);
            row.setAggregateId(orderNumber);
            row.setDestinationTopic(topic);
            row.setPayload(mapper.writeValueAsString(payload));
            row.setStatus("PENDING");
            outbox.save(row);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize payment outbox payload", ex);
        }
    }
}
