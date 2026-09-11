package com.pharmacy.drugstore.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {
    private static final Logger log = LoggerFactory.getLogger(NotificationFactory.class);
    private final MailClient mailClient;

    public NotificationFactory(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    public OrderNotification create(NotificationKind kind) {
        log.info("Notification create kind={}", kind);
        return switch (kind) {
            case PAYMENT_SUCCESS -> new PaymentSuccessEmailNotification(mailClient);
            case PAYMENT_FAILURE -> new PaymentFailureEmailNotification(mailClient);
            case ORDER_DETAILS -> new OrderDetailsEmailNotification(mailClient);
        };
    }
}
