package com.pharmacy.shipment.reliability;


import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
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
import com.pharmacy.shipment.parallel.StructuredFanout;

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
        if (batch.isEmpty()) {
            return;
        }
        Map<String, List<OutboxEvent>> groups = groupByAggregate(batch);
        if (groups.size() < 2) {
            for (OutboxEvent event : batch) {
                publishOne(event);
            }
            return;
        }
        List<Callable<List<SendOutcome>>> calls = new ArrayList<>();
        for (List<OutboxEvent> group : groups.values()) {
            List<SendRequest> requests = new ArrayList<>();
            for (OutboxEvent event : group) {
                requests.add(SendRequest.from(event));
            }
            calls.add(() -> sendGroup(requests));
        }
        Map<Long, SendOutcome> outcomes = new LinkedHashMap<>();
        for (List<SendOutcome> groupResult : StructuredFanout.all(calls)) {
            for (SendOutcome outcome : groupResult) {
                outcomes.put(outcome.id(), outcome);
            }
        }
        for (OutboxEvent event : batch) {
            SendOutcome outcome = outcomes.get(event.getId());
            if (outcome == null || outcome.error() != null) {
                markFailed(event, outcome == null
                        ? new IllegalStateException("Outbox publish result missing")
                        : outcome.error());
            } else {
                markSent(event);
            }
        }
    }

    private void publishOne(OutboxEvent event) {
        try {
            dispatch(SendRequest.from(event));
            markSent(event);
        } catch (Exception ex) {
            markFailed(event, ex);
        }
    }

    private List<SendOutcome> sendGroup(List<SendRequest> requests) {
        List<SendOutcome> outcomes = new ArrayList<>();
        for (SendRequest request : requests) {
            try {
                dispatch(request);
                outcomes.add(SendOutcome.sent(request));
            } catch (Exception ex) {
                outcomes.add(SendOutcome.failed(request, ex));
            }
        }
        return outcomes;
    }

    private void dispatch(SendRequest request) throws Exception {
        Object value = mapper.readValue(request.payload(), Object.class);
        kafka.send(request.topic(), request.aggregateId(), value).get(5, TimeUnit.SECONDS);
    }

    private static void markSent(OutboxEvent event) {
        event.setStatus("SENT");
        event.setPublishedAt(Instant.now());
        event.setLastError(null);
    }

    private void markFailed(OutboxEvent event, Exception ex) {
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(ex.getMessage());
        if (event.getAttempts() >= 25) {
            event.setStatus("FAILED");
        }
        log.error("Shipment outbox publish failed eventType={} orderNumber={}",
                event.getEventType(), event.getAggregateId(), ex);
    }

    private static Map<String, List<OutboxEvent>> groupByAggregate(List<OutboxEvent> batch) {
        Map<String, List<OutboxEvent>> groups = new LinkedHashMap<>();
        for (OutboxEvent event : batch) {
            String key = event.getAggregateId();
            if (key == null || key.isBlank()) {
                key = "event-" + event.getId();
            }
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(event);
        }
        return groups;
    }

    private record SendRequest(Long id, String aggregateId, String topic, String payload) {
        static SendRequest from(OutboxEvent event) {
            return new SendRequest(event.getId(), event.getAggregateId(), event.getDestinationTopic(), event.getPayload());
        }
    }

    private record SendOutcome(Long id, Exception error) {
        static SendOutcome sent(SendRequest request) {
            return new SendOutcome(request.id(), null);
        }

        static SendOutcome failed(SendRequest request, Exception error) {
            return new SendOutcome(request.id(), error);
        }
    }
}
