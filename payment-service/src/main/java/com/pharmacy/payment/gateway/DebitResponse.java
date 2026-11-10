package com.pharmacy.payment.gateway;

import java.math.BigDecimal;

public record DebitResponse(
        boolean success,
        String message,
        String gatewayRef,
        BigDecimal debitedAmount,
        BigDecimal availableBalance,
        BigDecimal creditedAmount,
        BigDecimal pharmacyBalance
) {}
