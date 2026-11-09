package com.pharmacy.drugstore.kafka;

public record PaymentResult(
        String correlationId,
        String orderNumber,
        String transactionRef,
        boolean success,
        String gatewayRef,
        String message,
        String maskedInstrument
) {}
