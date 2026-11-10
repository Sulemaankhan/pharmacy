package com.pharmacy.drugstore.kafka;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.OrderItem;
import com.pharmacy.drugstore.entity.PaymentTransaction;
import com.pharmacy.drugstore.entity.Shipment;
import com.pharmacy.drugstore.entity.User;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class NotificationCommands {
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.of("Asia/Kolkata"));

    private NotificationCommands() {}

    public static NotificationCommand forOrder(String kind, User user, CustomerOrder order, PaymentTransaction txn) {
        PaymentTransaction payment = txn != null ? txn : (order == null ? null : order.getPayment());
        Shipment ship = order == null ? null : order.getShipment();
        List<NotificationCommand.Item> items = new ArrayList<>();
        if (order != null && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                items.add(new NotificationCommand.Item(item.getProductName(), item.getQuantity(), item.getUnitPrice()));
            }
        }
        String shipmentStatus = ship == null || ship.getStatus() == null
                ? null : ship.getStatus().name().replace('_', ' ');
        return new NotificationCommand(
                kind,
                user == null ? null : user.getName(),
                user == null ? null : user.getEmail(),
                recipients(user, order),
                order == null ? null : order.getOrderNumber(),
                order == null || order.getStatus() == null ? null : order.getStatus().name(),
                order == null || order.getCreatedAt() == null ? null : WHEN.format(order.getCreatedAt()),
                payment == null || payment.getPaymentMode() == null ? null : payment.getPaymentMode().name(),
                payment == null ? null : payment.getTransactionRef(),
                payment == null ? null : payment.getMaskedInstrument(),
                payment == null ? null : payment.getGatewayMessage(),
                order == null ? null : order.getSubtotal(),
                order == null ? null : order.getShipping(),
                order == null ? null : order.getTotal(),
                List.copyOf(items),
                ship == null ? null : ship.getTrackingNumber(),
                ship == null ? null : ship.getRecipientName(),
                ship == null ? null : ship.getFullAddress(),
                ship == null ? null : ship.getContactNumber(),
                ship == null ? null : ship.getEmail(),
                shipmentStatus,
                null);
    }

    public static NotificationCommand passwordReset(User user, String resetUrl) {
        List<String> to = user != null && user.getEmail() != null && !user.getEmail().isBlank()
                ? List.of(user.getEmail().trim()) : List.of();
        return new NotificationCommand(
                "PASSWORD_RESET",
                user == null ? null : user.getName(),
                user == null ? null : user.getEmail(),
                to,
                null, null, null, null, null, null, null, null, null, null, List.of(),
                null, null, null, null, null, null,
                resetUrl);
    }

    public static List<String> recipients(User user, CustomerOrder order) {
        LinkedHashSet<String> to = new LinkedHashSet<>();
        if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            to.add(user.getEmail().trim());
        }
        if (order != null && order.getShipment() != null && order.getShipment().getEmail() != null
                && !order.getShipment().getEmail().isBlank()) {
            to.add(order.getShipment().getEmail().trim());
        }
        return List.copyOf(to);
    }
}
