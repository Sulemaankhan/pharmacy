package com.pharmacy.payment.strategy;

import com.pharmacy.payment.payment.PaymentMode;

public interface PaymentStrategy {
    PaymentMode mode();
    PaymentOutcome process(PaymentContext context);
}
