package com.pharmacy.drugstore.notification;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.OrderItem;
import com.pharmacy.drugstore.entity.PaymentTransaction;
import com.pharmacy.drugstore.entity.Shipment;
import com.pharmacy.drugstore.entity.User;
import java.math.BigDecimal;

public class PaymentSuccessEmailNotification implements OrderNotification {
    private final MailClient mailClient;

    public PaymentSuccessEmailNotification(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    @Override
    public void send(User user, CustomerOrder order, PaymentTransaction transaction) {
        String subject = "Order successful - " + order.getOrderNumber();
        mailClient.send("PAYMENT_SUCCESS", OrderEmailRecipients.of(user, order), subject, body(user, order, transaction));
    }

    private static String body(User user, CustomerOrder order, PaymentTransaction transaction) {
        StringBuilder items = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                BigDecimal line = item.getUnitPrice() == null
                        ? BigDecimal.ZERO
                        : item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                items.append("  - ").append(item.getProductName())
                        .append(" x ").append(item.getQuantity())
                        .append("  Rs.").append(line).append('\n');
            }
        }
        PaymentTransaction payment = transaction != null ? transaction : order.getPayment();
        Shipment ship = order.getShipment();
        String delivery = "Not available yet.";
        if (ship != null) {
            delivery = "Tracking ID: " + dash(ship.getTrackingNumber())
                    + "\nDeliver to: " + dash(ship.getRecipientName())
                    + "\nAddress: " + dash(ship.getFullAddress())
                    + "\nContact: " + dash(ship.getContactNumber())
                    + "\nEmail: " + dash(ship.getEmail());
        }
        return "Hello " + dash(user.getName()) + ",\n\n"
                + "Your Medicine Drugstore order was successful.\n\n"
                + "Order: " + dash(order.getOrderNumber()) + "\n"
                + "Total: Rs." + (order.getTotal() == null ? "0.00" : order.getTotal().toPlainString()) + "\n"
                + "Payment mode: " + (payment == null || payment.getPaymentMode() == null ? "-" : payment.getPaymentMode().name()) + "\n"
                + "Transaction: " + (payment == null ? "-" : dash(payment.getTransactionRef())) + "\n"
                + "Paid via: " + (payment == null ? "-" : dash(payment.getMaskedInstrument())) + "\n\n"
                + "Items\n" + (items.length() == 0 ? "  - No line items\n" : items)
                + "\nDelivery\n" + delivery + "\n\n"
                + "Thank you for shopping at Medicine Drugstore.\n";
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
