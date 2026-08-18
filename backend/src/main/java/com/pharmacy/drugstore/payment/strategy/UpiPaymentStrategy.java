package com.pharmacy.drugstore.payment.strategy;

import com.pharmacy.drugstore.payment.PaymentMode;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class UpiPaymentStrategy implements PaymentStrategy {
    @Override
    public PaymentMode mode() {
        return PaymentMode.UPI;
    }

    @Override
    public PaymentOutcome process(PaymentContext context) {
        String upi = context.details() == null ? null : context.details().upiId();
        if (upi == null || !upi.matches("[\\w.\\-]+@[\\w.\\-]+")) {
            return PaymentOutcome.failed("Enter a valid UPI ID such as name@okaxis");
        }
        if (upi.toLowerCase().contains("fail")) {
            return PaymentOutcome.failed("UPI collection was declined by the provider");
        }
        String masked = upi.replaceAll(".(?=.*@)", "*");
        return PaymentOutcome.success("UPI-" + ref(), "UPI payment captured", masked);
    }

    private String ref() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
