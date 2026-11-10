package com.pharmacy.drugstore.kafka;

import java.math.BigDecimal;

public record PaymentCommand(
        String correlationId,
        String orderNumber,
        String transactionRef,
        String paymentMode,
        BigDecimal amount,
        Long userId,
        String customerName,
        String customerEmail,
        String upiId,
        String cardNumber,
        String cardHolder,
        String expiry,
        String cvv,
        String bankName,
        String walletProvider,
        String walletPhone,
        String action
) {}
