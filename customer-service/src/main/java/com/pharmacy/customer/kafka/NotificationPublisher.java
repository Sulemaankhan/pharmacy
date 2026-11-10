package com.pharmacy.customer.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
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

    public void passwordReset(String name, String email, String resetUrl) {
        NotificationCommand command = new NotificationCommand(
                "PASSWORD_RESET",
                name,
                email,
                email == null || email.isBlank() ? List.of() : List.of(email.trim()),
                null, null, null, null, null, null, null, null, null, null, List.of(),
                null, null, null, null, null, null,
                resetUrl);
        try {
            kafka.send(topic, email, command).get(5, TimeUnit.SECONDS);
            log.info("Password reset notification queued email={}", email);
        } catch (Exception ex) {
            log.error("Password reset notification failed email={}", email, ex);
            throw new IllegalStateException("Notification service is unavailable. Please try again.", ex);
        }
    }
}
