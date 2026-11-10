package com.pharmacy.gateway.dto;

import java.math.BigDecimal;

public record DebitRequest(
        Long userId,
        String email,
        String customerName,
        BigDecimal amount,
        String orderNumber,
        String transactionRef,
        String paymentMode
) {}
