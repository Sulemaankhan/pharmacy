package com.pharmacy.drugstore.notification;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.PaymentTransaction;
import com.pharmacy.drugstore.entity.User;

public class PaymentSuccessEmailNotification implements OrderNotification {
    private final MailClient mailClient;

    public PaymentSuccessEmailNotification(MailClient mailClient) {
        this.mailClient = mailClient;
    }

    @Override
    public void send(User user, CustomerOrder order, PaymentTransaction transaction) {
        String subject = "Payment successful • Order " + order.getOrderNumber();
        String body = """
                Hello %s,

                Your payment of ₹%s for order %s is successful.

                Transaction ID: %s
                Payment mode: %s
                Paid via: %s

                Thank you for shopping at Medicine Drugstore.
                """.formatted(
                user.getName(),
                order.getTotal(),
                order.getOrderNumber(),
                transaction.getTransactionRef(),
                transaction.getPaymentMode(),
                transaction.getMaskedInstrument());
        mailClient.send("PAYMENT_SUCCESS", user.getEmail(), subject, body);
    }
}
