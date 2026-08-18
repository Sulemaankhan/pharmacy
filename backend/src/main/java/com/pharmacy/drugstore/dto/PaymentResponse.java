package com.pharmacy.drugstore.dto;

import com.pharmacy.drugstore.payment.PaymentMode;
import com.pharmacy.drugstore.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentResponse(
        boolean success,
        Long orderId,
        Long paymentId,
        String orderNumber,
        String transactionRef,
        PaymentMode paymentMode,
        PaymentStatus status,
        BigDecimal amount,
        boolean ledgerPosted,
        String message,
        String emailStatus,
        Instant paidAt,
        List<Item> items
) {
    public record Item(String name, int quantity, BigDecimal unitPrice) {}
}
