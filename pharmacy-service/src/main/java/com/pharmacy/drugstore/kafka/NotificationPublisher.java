package com.pharmacy.drugstore.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationPublisher {
    private static final Logger log = LoggerFactory.getLogger(NotificationPublisher.class);
    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public NotificationPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.notification-commands}") String topic) {
        this.kafka = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(NotificationCommand command) {
        String key = command.orderNumber() != null && !command.orderNumber().isBlank()
                ? command.orderNumber()
                : command.customerEmail();
        log.info("Notification command published kind={} key={} topic={}", command.kind(), key, topic);
        try {
            kafka.send(topic, key, command).get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            log.error("Notification kafka publish failed kind={} key={}", command.kind(), key, ex);
            throw new IllegalStateException("Notification service is unavailable. Please try again.", ex);
        }
    }
}
