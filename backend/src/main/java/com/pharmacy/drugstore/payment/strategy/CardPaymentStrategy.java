package com.pharmacy.drugstore.payment.strategy;

import com.pharmacy.drugstore.payment.PaymentMode;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class CardPaymentStrategy implements PaymentStrategy {
    @Override
    public PaymentMode mode() {
        return PaymentMode.CARD;
    }

    @Override
    public PaymentOutcome process(PaymentContext context) {
        var d = context.details();
        String number = d == null || d.cardNumber() == null ? "" : d.cardNumber().replaceAll("\\s", "");
        String holder = d == null ? null : d.cardHolder();
        String expiry = d == null ? null : d.expiry();
        String cvv = d == null ? null : d.cvv();

        if (holder == null || holder.isBlank()) {
            return PaymentOutcome.failed("Card holder name is required");
        }
        if (!number.matches("\\d{16}")) {
            return PaymentOutcome.failed("Enter a 16-digit card number");
        }
        if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return PaymentOutcome.failed("Enter expiry as MM/YY");
        }
        if (cvv == null || !cvv.matches("\\d{3}")) {
            return PaymentOutcome.failed("Enter a 3-digit CVV");
        }
        if ("4000000000000002".equals(number)) {
            return PaymentOutcome.failed("Card was declined by the issuing bank");
        }
        String last4 = number.substring(number.length() - 4);
        return PaymentOutcome.success("CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Card payment authorized", "**** **** **** " + last4);
    }
}
