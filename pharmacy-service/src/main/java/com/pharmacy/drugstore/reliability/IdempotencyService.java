package com.pharmacy.drugstore.reliability;

import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {
    private final ProcessedEventRepository processed;

    public IdempotencyService(ProcessedEventRepository processed) {
        this.processed = processed;
    }

    public boolean markIfNew(String consumer, String eventId, String eventType, String aggregateId) {
        if (eventId == null || eventId.isBlank()) {
            return true;
        }
        if (processed.existsByConsumerNameAndEventId(consumer, eventId)) {
            return false;
        }
        ProcessedEvent row = new ProcessedEvent();
        row.setConsumerName(consumer);
        row.setEventId(eventId);
        row.setEventType(eventType);
        row.setAggregateId(aggregateId);
        processed.save(row);
        return true;
    }
}
