package com.pharmacy.payment.event;

public record PaymentResult(
        String correlationId,
        String orderNumber,
        String transactionRef,
        boolean success,
        String gatewayRef,
        String message,
        String maskedInstrument
) {}
