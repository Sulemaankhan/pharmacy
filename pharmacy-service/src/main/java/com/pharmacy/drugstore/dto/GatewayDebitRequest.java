package com.pharmacy.drugstore.dto;

import java.math.BigDecimal;

public record GatewayDebitRequest(
        Long userId,
        String email,
        String customerName,
        BigDecimal amount,
        String orderNumber,
        String transactionRef,
        String paymentMode
) {}
