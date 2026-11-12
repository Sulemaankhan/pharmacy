package com.pharmacy.shipment.reliability;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;

    public OutboxPublisher(OutboxEventRepository outbox, KafkaTemplate<String, Object> kafka, ObjectMapper mapper) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.mapper = mapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:400}")
    @Transactional
    public void flush() {
        List<OutboxEvent> batch = outbox.findTop50ByStatusOrderByIdAsc("PENDING");
        for (OutboxEvent event : batch) {
            try {
                Object value = mapper.readValue(event.getPayload(), Object.class);
                kafka.send(event.getDestinationTopic(), event.getAggregateId(), value).get(5, TimeUnit.SECONDS);
                event.setStatus("SENT");
                event.setPublishedAt(Instant.now());
                event.setLastError(null);
            } catch (Exception ex) {
                event.setAttempts(event.getAttempts() + 1);
                event.setLastError(ex.getMessage());
                if (event.getAttempts() >= 25) {
                    event.setStatus("FAILED");
                }
                log.error("Shipment outbox publish failed eventType={} orderNumber={}",
                        event.getEventType(), event.getAggregateId(), ex);
            }
        }
    }
}
