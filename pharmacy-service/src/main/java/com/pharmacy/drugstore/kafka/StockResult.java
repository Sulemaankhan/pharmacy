package com.pharmacy.drugstore.kafka;

public record StockResult(
        String correlationId,
        String orderNumber,
        boolean success,
        String message
) {}
