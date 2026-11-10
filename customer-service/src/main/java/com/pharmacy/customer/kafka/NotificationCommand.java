package com.pharmacy.customer.kafka;

import java.math.BigDecimal;
import java.util.List;

public record NotificationCommand(
        String kind,
        String customerName,
        String customerEmail,
        List<String> recipients,
        String orderNumber,
        String orderStatus,
        String placedAt,
        String paymentMode,
        String transactionRef,
        String maskedInstrument,
        String gatewayMessage,
        BigDecimal subtotal,
        BigDecimal shipping,
        BigDecimal total,
        List<Item> items,
        String trackingNumber,
        String recipientName,
        String fullAddress,
        String contactNumber,
        String shipmentEmail,
        String shipmentStatus,
        String resetUrl
) {
    public record Item(String productName, int quantity, BigDecimal unitPrice) {}
}
