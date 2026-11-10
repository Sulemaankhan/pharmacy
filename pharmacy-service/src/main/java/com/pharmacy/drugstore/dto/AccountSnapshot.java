package com.pharmacy.drugstore.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AccountSnapshot(
        Long accountId,
        Long userId,
        String email,
        String customerName,
        String accountType,
        BigDecimal availableBalance,
        Instant updatedAt,
        List<AccountTxn> transactions
) {
    public record AccountTxn(
            Long id,
            String type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String orderNumber,
            String transactionRef,
            String paymentMode,
            String description,
            Instant createdAt
    ) {}
}
