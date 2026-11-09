package com.pharmacy.shipment.mapper;

import com.pharmacy.shipment.entity.Shipment;
import com.pharmacy.shipment.event.ShipmentUpdate;

public final class ShipmentMapper {
    private ShipmentMapper() {}

    public static ShipmentUpdate toUpdate(Shipment shipment) {
        if (shipment == null) {
            return null;
        }
        return new ShipmentUpdate(
                shipment.getId(),
                shipment.getUserId(),
                shipment.getOrderId(),
                shipment.getOrderNumber(),
                shipment.getTrackingNumber(),
                shipment.getRecipientName(),
                shipment.getAddress(),
                shipment.getCity(),
                shipment.getState(),
                shipment.getPincode(),
                shipment.getContactNumber(),
                shipment.getEmail(),
                shipment.getFullAddress(),
                shipment.getStatus() == null ? null : shipment.getStatus().name(),
                shipment.getCarrier(),
                shipment.getEstimatedDelivery(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt());
    }
}
