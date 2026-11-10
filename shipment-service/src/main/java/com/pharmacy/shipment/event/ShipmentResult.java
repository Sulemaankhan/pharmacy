package com.pharmacy.shipment.event;

public record ShipmentResult(
        String correlationId,
        String orderNumber,
        String trackingNumber,
        boolean success,
        String status,
        String message
) {
    public static ShipmentResult ok(ShipmentCommand command, String status, String message) {
        return new ShipmentResult(
                command == null ? null : command.correlationId(),
                command == null ? null : command.orderNumber(),
                command == null ? null : command.trackingNumber(),
                true,
                status,
                message);
    }

    public static ShipmentResult fail(ShipmentCommand command, String message) {
        return new ShipmentResult(
                command == null ? null : command.correlationId(),
                command == null ? null : command.orderNumber(),
                command == null ? null : command.trackingNumber(),
                false,
                "UNAVAILABLE",
                message);
    }
}
