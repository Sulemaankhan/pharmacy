package com.pharmacy.payment.event;

import java.math.BigDecimal;

public record PaymentCommand(
        String correlationId,
        String orderNumber,
        String transactionRef,
        String paymentMode,
        BigDecimal amount,
        String customerName,
        String customerEmail,
        String upiId,
        String cardNumber,
        String cardHolder,
        String expiry,
        String cvv,
        String bankName,
        String walletProvider,
        String walletPhone
) {}
