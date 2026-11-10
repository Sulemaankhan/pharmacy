package com.pharmacy.payment.event;

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
    public static PaymentResult fail(PaymentCommand command, String message) {
        return fail(command, message, null, null);
    }

    public static PaymentResult fail(PaymentCommand command, String message, String masked, BigDecimal available) {
        return new PaymentResult(
                command == null ? null : command.correlationId(),
                command == null ? null : command.orderNumber(),
                command == null ? null : command.transactionRef(),
                false,
                null,
                message,
                masked,
                BigDecimal.ZERO,
                available,
                BigDecimal.ZERO,
                null);
    }
}
