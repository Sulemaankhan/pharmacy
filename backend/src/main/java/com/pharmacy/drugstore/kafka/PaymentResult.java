package com.pharmacy.drugstore.kafka;

import java.math.BigDecimal;

public record PaymentResult(
        String correlationId,
        String orderNumber,
        String transactionRef,
        boolean success,
        String gatewayRef,
        String message,
        String maskedInstrument,
        BigDecimal debitedAmount,
        BigDecimal availableBalance,
        BigDecimal creditedAmount,
        BigDecimal pharmacyBalance
) {
    public static PaymentResult fail(String message) {
        return new PaymentResult(null, null, null, false, null, message, null, null, null, null, null);
    }
}
