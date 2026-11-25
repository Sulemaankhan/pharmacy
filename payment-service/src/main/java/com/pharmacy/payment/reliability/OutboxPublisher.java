package com.pharmacy.payment.reliability;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;

    public OutboxPublisher(
            OutboxEventRepository outbox,
            KafkaTemplate<String, Object> kafka,
            ObjectMapper mapper,
            PlatformTransactionManager transactionManager) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public void publishAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            flush();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                flush();
            }
        });
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:50}")
    public void flush() {
        synchronized (this) {
            tx.executeWithoutResult(status -> publishPending());
        }
    }

    private void publishPending() {
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
                log.error("Payment outbox publish failed eventType={} orderNumber={}",
                        event.getEventType(), event.getAggregateId(), ex);
            }
        }
    }
}
