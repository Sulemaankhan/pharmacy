package com.pharmacy.gateway.dto;

import java.math.BigDecimal;

public record DebitResponse(
        boolean success,
        String message,
        String gatewayRef,
        BigDecimal debitedAmount,
        BigDecimal availableBalance,
        BigDecimal creditedAmount,
        BigDecimal pharmacyBalance
) {
    public static DebitResponse ok(
            String gatewayRef,
            BigDecimal debited,
            BigDecimal customerBalance,
            BigDecimal credited,
            BigDecimal pharmacyBalance) {
        return new DebitResponse(true, "Account debited and pharmacy credited",
                gatewayRef, debited, customerBalance, credited, pharmacyBalance);
    }

    public static DebitResponse insufficient(BigDecimal available, BigDecimal required) {
        return new DebitResponse(
                false,
                "Insufficient balance for this order.",
                null,
                BigDecimal.ZERO,
                available,
                BigDecimal.ZERO,
                null);
    }

    public static DebitResponse failed(String message, BigDecimal available) {
        return new DebitResponse(false, message, null, BigDecimal.ZERO, available, BigDecimal.ZERO, null);
    }

    public static DebitResponse refunded(
            String gatewayRef,
            BigDecimal refunded,
            BigDecimal customerBalance,
            BigDecimal pharmacyBalance) {
        return new DebitResponse(true, "Amount refunded to customer and reversed from pharmacy",
                gatewayRef, BigDecimal.ZERO, customerBalance, refunded, pharmacyBalance);
    }
}
