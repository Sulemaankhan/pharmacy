package com.pharmacy.drugstore.payment.strategy;

public record PaymentOutcome(boolean success, String gatewayRef, String message, String maskedInstrument) {
    public static PaymentOutcome success(String gatewayRef, String message, String maskedInstrument) {
        return new PaymentOutcome(true, gatewayRef, message, maskedInstrument);
    }

    public static PaymentOutcome failed(String message) {
        return new PaymentOutcome(false, null, message, null);
    }
}
