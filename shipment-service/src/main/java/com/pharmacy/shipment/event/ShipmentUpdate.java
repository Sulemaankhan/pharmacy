package com.pharmacy.shipment.event;

import java.time.Instant;

public record ShipmentUpdate(
        Long id,
        Long userId,
        Long orderId,
        String orderNumber,
        String trackingNumber,
        String recipientName,
        String address,
        String city,
        String state,
        String pincode,
        String contactNumber,
        String email,
        String fullAddress,
        String status,
        String carrier,
        Instant estimatedDelivery,
        Instant createdAt,
        Instant updatedAt,
        Instant shippedAt,
        Instant deliveredAt
) {}
