package com.pharmacy.drugstore.kafka;

public record ShipmentResult(
        String correlationId,
        String orderNumber,
        String trackingNumber,
        boolean success,
        String status,
        String message
) {}
