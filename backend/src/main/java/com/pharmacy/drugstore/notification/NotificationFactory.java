package com.pharmacy.drugstore.notification;

import org.springframework.stereotype.Component;

@Component
public class NotificationFactory {
    private final MailClient mailClient;

    public NotificationFactory(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    public OrderNotification create(NotificationKind kind) {
        return switch (kind) {
            case PAYMENT_SUCCESS -> new PaymentSuccessEmailNotification(mailClient);
            case PAYMENT_FAILURE -> new PaymentFailureEmailNotification(mailClient);
        };
    }
}
