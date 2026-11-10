package com.pharmacy.shipment.event;

import java.time.Instant;

public record ShipmentCommand(
        String type,
        Long userId,
        String customerEmail,
        Long orderId,
        String orderNumber,
        String trackingNumber,
        Boolean paid,
        String recipientName,
        String address,
        String city,
        String state,
        String pincode,
        String contactNumber,
        String email,
        String carrier,
        Instant estimatedDelivery,
        Instant createdAt,
        String correlationId
) {}
