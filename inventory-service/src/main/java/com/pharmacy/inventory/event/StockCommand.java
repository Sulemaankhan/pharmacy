package com.pharmacy.inventory.event;

import java.util.List;

public record StockCommand(
        String action,
        String correlationId,
        String orderNumber,
        List<StockLine> items
) {
    public record StockLine(Long productId, int quantity, String productName) {}
}
