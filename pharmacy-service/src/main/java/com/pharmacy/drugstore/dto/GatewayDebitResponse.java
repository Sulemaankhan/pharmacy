package com.pharmacy.drugstore.dto;

import java.math.BigDecimal;

public record GatewayDebitResponse(
        boolean success,
        String message,
        String gatewayRef,
        BigDecimal debitedAmount,
        BigDecimal availableBalance,
        BigDecimal creditedAmount,
        BigDecimal pharmacyBalance
) {
    public static GatewayDebitResponse failed(String message) {
        return new GatewayDebitResponse(false, message, null, null, null, null, null);
    }

    public boolean moneyReturned() {
        return success && creditedAmount != null && creditedAmount.signum() > 0;
    }
}
