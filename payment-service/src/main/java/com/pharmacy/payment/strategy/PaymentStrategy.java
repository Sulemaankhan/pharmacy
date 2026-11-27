package com.pharmacy.payment.strategy;

import com.pharmacy.payment.payment.PaymentMode;

public sealed interface PaymentStrategy
        permits CardPaymentStrategy, CodPaymentStrategy, NetBankingPaymentStrategy, UpiPaymentStrategy, WalletPaymentStrategy {
    PaymentMode mode();
    PaymentOutcome process(PaymentContext context);
}
