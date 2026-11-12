package com.pharmacy.notification.email;

import com.pharmacy.notification.event.NotificationCommand;
import java.math.BigDecimal;
import java.util.List;

final class EmailBodies {
    private EmailBodies() {}

    static String paymentSuccess(NotificationCommand cmd) {
        return "Hello " + dash(cmd.customerName()) + ",\n\n"
                + "Your Medicine Drugstore order was successful.\n\n"
                + "Order: " + dash(cmd.orderNumber()) + "\n"
                + "Total: Rs." + money(cmd.total()) + "\n"
                + "Payment mode: " + dash(cmd.paymentMode()) + "\n"
                + "Transaction: " + dash(cmd.transactionRef()) + "\n"
                + "Paid via: " + dash(cmd.maskedInstrument()) + "\n\n"
                + "Items\n" + items(cmd.items())
                + "\nDelivery\n" + delivery(cmd) + "\n\n"
                + "Thank you for shopping at Medicine Drugstore.\n";
    }

    static String paymentFailure(NotificationCommand cmd) {
        String reason = cmd.gatewayMessage() == null || cmd.gatewayMessage().isBlank()
                ? "The order was cancelled." : cmd.gatewayMessage();
        return "Hello " + dash(cmd.customerName()) + ",\n\n"
                + "Your Medicine Drugstore order could not be completed and was cancelled.\n\n"
                + "Order: " + dash(cmd.orderNumber()) + "\n"
                + "Total: Rs." + money(cmd.total()) + "\n"
                + "Payment mode: " + dash(cmd.paymentMode()) + "\n"
                + "Transaction: " + dash(cmd.transactionRef()) + "\n"
                + "Reason: " + reason + "\n\n"
                + "Items\n" + items(cmd.items())
                + "\nDelivery\n" + delivery(cmd) + "\n\n"
                + "If a payment was taken, it has been refunded to your account.\n\n"
                + "Medicine Drugstore\n";
    }

    static String orderDetails(NotificationCommand cmd) {
        return "Hello " + dash(cmd.customerName()) + ",\n\n"
                + "Here are the details for your Medicine Drugstore order.\n\n"
                + "Order: " + dash(cmd.orderNumber()) + "\n"
                + "Placed: " + dash(cmd.placedAt()) + "\n"
                + "Status: " + dash(cmd.orderStatus()) + "\n\n"
                + "Items\n" + items(cmd.items()).stripTrailing() + "\n"
                + "Subtotal: Rs." + money(cmd.subtotal()) + "\n"
                + "Shipping: Rs." + money(cmd.shipping()) + "\n"
                + "Total: Rs." + money(cmd.total()) + "\n\n"
                + "Payment\n"
                + "Mode: " + dash(cmd.paymentMode()) + "\n"
                + "Transaction: " + dash(cmd.transactionRef()) + "\n\n"
                + "Delivery\n" + deliveryWithStatus(cmd) + "\n"
                + "This copy was sent to your account email: " + dash(cmd.customerEmail()) + "\n\n"
                + "Thank you for shopping at Medicine Drugstore.\n";
    }

    static String shipmentPickedUp(NotificationCommand cmd) {
        return "Hello " + dash(cmd.customerName()) + ",\n\n"
                + "Your Medicine Drugstore order has been picked up and is on the way.\n\n"
                + "Order: " + dash(cmd.orderNumber()) + "\n"
                + "Tracking ID: " + dash(cmd.trackingNumber()) + "\n"
                + "Status: " + dash(cmd.shipmentStatus()) + "\n\n"
                + "Delivery\n" + delivery(cmd) + "\n\n"
                + "We will email you again when it is delivered.\n\n"
                + "Medicine Drugstore\n";
    }

    static String orderDelivered(NotificationCommand cmd) {
        return "Hello " + dash(cmd.customerName()) + ",\n\n"
                + "Your Medicine Drugstore order has been delivered.\n\n"
                + "Order: " + dash(cmd.orderNumber()) + "\n"
                + "Tracking ID: " + dash(cmd.trackingNumber()) + "\n"
                + "Status: DELIVERED\n\n"
                + "Delivery\n" + delivery(cmd) + "\n\n"
                + "Thank you for shopping at Medicine Drugstore.\n";
    }

    static String passwordReset(NotificationCommand cmd) {
        return "Hello " + dash(cmd.customerName()) + ",\n\n"
                + "We received a request to reset the password for " + dash(cmd.customerEmail()) + ".\n"
                + "Open this link to choose a new password. It expires in 30 minutes.\n\n"
                + dash(cmd.resetUrl()) + "\n\n"
                + "If you did not ask for this, you can ignore this email.\n\n"
                + "Medicine Drugstore\n";
    }

    static List<String> recipients(NotificationCommand cmd) {
        if (cmd.recipients() != null && !cmd.recipients().isEmpty()) {
            return cmd.recipients();
        }
        if (cmd.customerEmail() != null && !cmd.customerEmail().isBlank()) {
            return List.of(cmd.customerEmail());
        }
        return List.of();
    }

    private static String items(List<NotificationCommand.Item> items) {
        if (items == null || items.isEmpty()) {
            return "  - No line items\n";
        }
        StringBuilder sb = new StringBuilder();
        for (NotificationCommand.Item item : items) {
            BigDecimal line = item.unitPrice() == null
                    ? BigDecimal.ZERO
                    : item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            sb.append("  - ").append(item.productName())
                    .append(" x ").append(item.quantity())
                    .append("  Rs.").append(line).append('\n');
        }
        return sb.toString();
    }

    private static String delivery(NotificationCommand cmd) {
        if (blank(cmd.trackingNumber()) && blank(cmd.recipientName()) && blank(cmd.fullAddress())) {
            return "Not available yet.";
        }
        return "Tracking ID: " + dash(cmd.trackingNumber())
                + "\nDeliver to: " + dash(cmd.recipientName())
                + "\nAddress: " + dash(cmd.fullAddress())
                + "\nContact: " + dash(cmd.contactNumber())
                + "\nEmail: " + dash(cmd.shipmentEmail());
    }

    private static String deliveryWithStatus(NotificationCommand cmd) {
        if (blank(cmd.trackingNumber()) && blank(cmd.recipientName()) && blank(cmd.fullAddress())) {
            return "Not available yet.";
        }
        return "Tracking ID: " + dash(cmd.trackingNumber())
                + "\nStatus: " + dash(cmd.shipmentStatus())
                + "\nDeliver to: " + dash(cmd.recipientName())
                + "\nAddress: " + dash(cmd.fullAddress())
                + "\nContact: " + dash(cmd.contactNumber())
                + "\nEmail: " + dash(cmd.shipmentEmail());
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
