package com.pharmacy.drugstore.notification;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.OrderItem;
import com.pharmacy.drugstore.entity.PaymentTransaction;
import com.pharmacy.drugstore.entity.Shipment;
import com.pharmacy.drugstore.entity.User;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class OrderDetailsEmailNotification implements OrderNotification {
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(ZoneId.of("Asia/Kolkata"));

    private final MailClient mailClient;

    public OrderDetailsEmailNotification(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    @Override
    public void send(User user, CustomerOrder order, PaymentTransaction transaction) {
        String subject = "Your order details - " + order.getOrderNumber();
        mailClient.send("ORDER_DETAILS", OrderEmailRecipients.of(user, order), subject, body(user, order, transaction));
    }

    private static String body(User user, CustomerOrder order, PaymentTransaction transaction) {
        StringBuilder items = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                BigDecimal line = item.getUnitPrice() == null
                        ? BigDecimal.ZERO
                        : item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                items.append("  - ")
                        .append(item.getProductName())
                        .append(" x ")
                        .append(item.getQuantity())
                        .append("  Rs.")
                        .append(line)
                        .append('\n');
            }
        }
        if (items.length() == 0) {
            items.append("  - No line items\n");
        }

        PaymentTransaction payment = transaction != null ? transaction : order.getPayment();
        Shipment ship = order.getShipment();
        String placed = order.getCreatedAt() == null ? "-" : WHEN.format(order.getCreatedAt());
        String shipmentText;
        if (ship == null) {
            shipmentText = "Not available yet.";
        } else {
            shipmentText = "Tracking ID: " + dash(ship.getTrackingNumber())
                    + "\nStatus: " + (ship.getStatus() == null ? "-" : ship.getStatus().name().replace('_', ' '))
                    + "\nDeliver to: " + dash(ship.getRecipientName())
                    + "\nAddress: " + dash(ship.getFullAddress())
                    + "\nContact: " + dash(ship.getContactNumber())
                    + "\nEmail: " + dash(ship.getEmail());
        }

        return "Hello " + dash(user.getName()) + ",\n\n"
                + "Here are the details for your Medicine Drugstore order.\n\n"
                + "Order: " + dash(order.getOrderNumber()) + "\n"
                + "Placed: " + placed + "\n"
                + "Status: " + (order.getStatus() == null ? "-" : order.getStatus().name()) + "\n\n"
                + "Items\n" + items.toString().stripTrailing() + "\n"
                + "Subtotal: Rs." + money(order.getSubtotal()) + "\n"
                + "Shipping: Rs." + money(order.getShipping()) + "\n"
                + "Total: Rs." + money(order.getTotal()) + "\n\n"
                + "Payment\n"
                + "Mode: " + (payment == null || payment.getPaymentMode() == null ? "-" : payment.getPaymentMode().name()) + "\n"
                + "Transaction: " + (payment == null ? "-" : dash(payment.getTransactionRef())) + "\n\n"
                + "Delivery\n" + shipmentText + "\n"
                + "This copy was sent to your account email: " + dash(user.getEmail()) + "\n\n"
                + "Thank you for shopping at Medicine Drugstore.\n";
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
