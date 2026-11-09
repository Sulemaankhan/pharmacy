package com.pharmacy.notification.email;

import com.pharmacy.notification.event.NotificationCommand;
import com.pharmacy.notification.mail.MailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationDispatcher {
    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private final MailClient mailClient;

    public NotificationDispatcher(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    public void send(NotificationCommand command) {
        if (command == null || command.kind() == null) {
            throw new IllegalArgumentException("Notification kind is required");
        }
        String kind = command.kind().trim().toUpperCase();
        log.info("Notification dispatch kind={} orderNumber={} to={}",
                kind, command.orderNumber(), EmailBodies.recipients(command));
        switch (kind) {
            case "PAYMENT_SUCCESS" -> mailClient.send(
                    kind, EmailBodies.recipients(command),
                    "Order successful - " + dash(command.orderNumber()),
                    EmailBodies.paymentSuccess(command));
            case "PAYMENT_FAILURE" -> mailClient.send(
                    kind, EmailBodies.recipients(command),
                    "Order payment failed - " + dash(command.orderNumber()),
                    EmailBodies.paymentFailure(command));
            case "ORDER_DETAILS" -> mailClient.send(
                    kind, EmailBodies.recipients(command),
                    "Your order details - " + dash(command.orderNumber()),
                    EmailBodies.orderDetails(command));
            case "PASSWORD_RESET" -> mailClient.send(
                    kind, EmailBodies.recipients(command),
                    "Reset your Medicine Drugstore password",
                    EmailBodies.passwordReset(command));
            default -> throw new IllegalArgumentException("Unsupported notification kind: " + kind);
        }
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
