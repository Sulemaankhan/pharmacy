package com.pharmacy.drugstore.notification;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.PaymentTransaction;
import com.pharmacy.drugstore.entity.User;

public class PaymentFailureEmailNotification implements OrderNotification {
    private final MailClient mailClient;

    public PaymentFailureEmailNotification(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    @Override
    public void send(User user, CustomerOrder order, PaymentTransaction transaction) {
        String subject = "Payment failed • Order " + order.getOrderNumber();
        String body = """
                Hello %s,

                We could not complete payment for order %s.
                Reason: %s

                Your cart is still saved. Please try another payment mode.
                """.formatted(user.getName(), order.getOrderNumber(), transaction.getGatewayMessage());
        mailClient.send("PAYMENT_FAILURE", user.getEmail(), subject, body);
    }
}
