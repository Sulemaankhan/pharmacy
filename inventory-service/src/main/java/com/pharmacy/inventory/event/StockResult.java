package com.pharmacy.inventory.event;

public record StockResult(
        String correlationId,
        String orderNumber,
        boolean success,
        String message
) {
    public static StockResult ok(StockCommand command, String message) {
        return new StockResult(
                command == null ? null : command.correlationId(),
                command == null ? null : command.orderNumber(),
                true,
                message);
    }

    public static StockResult fail(StockCommand command, String message) {
        return new StockResult(
                command == null ? null : command.correlationId(),
                command == null ? null : command.orderNumber(),
                false,
                message);
    }
}
