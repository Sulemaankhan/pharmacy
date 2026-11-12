package com.pharmacy.drugstore.reliability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.drugstore.kafka.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class OutboxService {
    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

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
            String id = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
            String json = mapper.writeValueAsString(payload);
            var existing = outbox.findByEventId(id);
            if (existing.isPresent()) {
                OutboxEvent row = existing.get();
                row.setEventType(eventType);
                row.setAggregateId(orderNumber);
                row.setDestinationTopic(topic);
                row.setPayload(json);
                row.setStatus("PENDING");
                row.setAttempts(0);
                row.setLastError(null);
                row.setPublishedAt(null);
                outbox.save(row);
                log.info("Outbox event requeued eventId={} eventType={} orderNumber={}", id, eventType, orderNumber);
                return;
            }
            OutboxEvent row = new OutboxEvent();
            row.setEventId(id);
            row.setEventType(eventType);
            row.setAggregateId(orderNumber);
            row.setDestinationTopic(topic);
            row.setPayload(json);
            row.setStatus("PENDING");
            outbox.save(row);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize outbox payload for " + eventType, ex);
        }
    }
}
