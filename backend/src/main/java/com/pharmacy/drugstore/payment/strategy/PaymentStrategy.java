package com.pharmacy.drugstore.payment.strategy;

import com.pharmacy.drugstore.payment.PaymentMode;

public interface PaymentStrategy {
    PaymentMode mode();
    PaymentOutcome process(PaymentContext context);
}
