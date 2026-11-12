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
                    kind, command.orderNumber(), EmailBodies.recipients(command),
                    "Order successful - " + dash(command.orderNumber()),
                    EmailBodies.paymentSuccess(command));
            case "PAYMENT_FAILURE" -> mailClient.send(
                    kind, command.orderNumber(), EmailBodies.recipients(command),
                    "Order failed - " + dash(command.orderNumber()),
                    EmailBodies.paymentFailure(command));
            case "ORDER_DETAILS" -> mailClient.send(
                    kind, command.orderNumber(), EmailBodies.recipients(command),
                    "Your order details - " + dash(command.orderNumber()),
                    EmailBodies.orderDetails(command));
            case "SHIPMENT_PICKED_UP" -> mailClient.send(
                    kind, command.orderNumber(), EmailBodies.recipients(command),
                    "Your order is on the way - " + dash(command.orderNumber()),
                    EmailBodies.shipmentPickedUp(command));
            case "ORDER_DELIVERED" -> mailClient.send(
                    kind, command.orderNumber(), EmailBodies.recipients(command),
                    "Your order was delivered - " + dash(command.orderNumber()),
                    EmailBodies.orderDelivered(command));
            case "PASSWORD_RESET" -> mailClient.send(
                    kind, command.orderNumber(), EmailBodies.recipients(command),
                    "Reset your Medicine Drugstore password",
                    EmailBodies.passwordReset(command));
            default -> throw new IllegalArgumentException("Unsupported notification kind: " + kind);
        }
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
