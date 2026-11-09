package com.pharmacy.notification.kafka;

import com.pharmacy.notification.email.NotificationDispatcher;
import com.pharmacy.notification.event.NotificationCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationCommandListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationCommandListener.class);
    private final NotificationDispatcher dispatcher;

    public NotificationCommandListener(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(topics = "${app.kafka.topics.notification-commands}", groupId = "notification-service")
    public void onCommand(NotificationCommand command) {
        if (command == null) {
            log.warn("Notification command was empty");
            return;
        }
        log.info("Notification command received kind={} orderNumber={}", command.kind(), command.orderNumber());
        try {
            dispatcher.send(command);
        } catch (Exception ex) {
            log.error("Notification failed kind={} orderNumber={} reason={}",
                    command.kind(), command.orderNumber(), ex.getMessage());
        }
    }
}
