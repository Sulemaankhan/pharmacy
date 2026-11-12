package com.pharmacy.drugstore.kafka;

import com.pharmacy.drugstore.reliability.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationPublisher {
    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);
    private final OutboxService outbox;
    private final String topic;

    public NotificationPublisher(
            OutboxService outbox,
            @Value("${app.kafka.topics.notification-commands}") String topic) {
        this.outbox = outbox;
        this.topic = topic;
    }

    public void publish(NotificationCommand command) {
        String key = command.orderNumber() != null && !command.orderNumber().isBlank()
                ? command.orderNumber()
                : command.customerEmail();
        log.info("Notification command queued kind={} key={} topic={}", command.kind(), key, topic);
        outbox.enqueue(topic, key, command.kind(), command);
    }
}
