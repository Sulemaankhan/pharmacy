package com.pharmacy.inventory.service;

import com.pharmacy.inventory.entity.Product;
import com.pharmacy.inventory.entity.StockReservation;
import com.pharmacy.inventory.event.StockCommand;
import com.pharmacy.inventory.event.StockResult;
import com.pharmacy.inventory.repository.ProductRepository;
import com.pharmacy.inventory.repository.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockService {
    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private final ProductRepository products;
    private final StockReservationRepository reservations;

    public StockService(ProductRepository products, StockReservationRepository reservations) {
        this.products = products;
        this.reservations = reservations;
    }

    @Transactional
    public StockResult handle(StockCommand command) {
        if (command == null || command.action() == null) {
            return StockResult.fail(command, "Stock command was empty");
        }
        String action = command.action().trim().toUpperCase();
        if ("RESERVE".equals(action)) {
            return reserve(command);
        }
        if ("RELEASE".equals(action)) {
            return release(command);
        }
        return StockResult.fail(command, "Unsupported stock action " + action);
    }

    private StockResult reserve(StockCommand command) {
        if (command.orderNumber() == null || command.orderNumber().isBlank()) {
            return StockResult.fail(command, "Order number is required");
        }
        var existing = reservations.findByOrderNumber(command.orderNumber());
        if (existing.isPresent() && "RESERVED".equals(existing.get().getStatus())) {
            return StockResult.ok(command, "Stock already reserved");
        }
        if (command.items() == null || command.items().isEmpty()) {
            return StockResult.fail(command, "Stock items are required");
        }
        Map<Long, Integer> qty = new LinkedHashMap<>();
        for (StockCommand.StockLine line : command.items()) {
            if (line == null || line.productId() == null || line.quantity() < 1) {
                return StockResult.fail(command, "Each stock line needs a product and quantity");
            }
            qty.merge(line.productId(), line.quantity(), Integer::sum);
        }
        Map<Long, Product> locked = products.lockAllById(qty.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        for (Map.Entry<Long, Integer> entry : qty.entrySet()) {
            Product product = locked.get(entry.getKey());
            if (product == null) {
                return StockResult.fail(command, "Product not found: " + entry.getKey());
            }
            int available = product.getStock() == null ? 0 : product.getStock();
            if (available < entry.getValue()) {
                return StockResult.fail(command, "Insufficient stock for " + product.getName());
            }
        }
        for (Map.Entry<Long, Integer> entry : qty.entrySet()) {
            Product product = locked.get(entry.getKey());
            product.setStock(product.getStock() - entry.getValue());
            products.save(product);
        }
        StockReservation reservation = existing.orElseGet(StockReservation::new);
        reservation.setOrderNumber(command.orderNumber());
        reservation.setStatus("RESERVED");
        reservation.setLines(qty.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(",")));
        reservations.save(reservation);
        log.info("Stock reserved orderNumber={} items={}", command.orderNumber(), qty);
        return StockResult.ok(command, "Stock reserved");
    }

    private StockResult release(StockCommand command) {
        if (command.orderNumber() == null || command.orderNumber().isBlank()) {
            return StockResult.fail(command, "Order number is required");
        }
        StockReservation reservation = reservations.findByOrderNumber(command.orderNumber()).orElse(null);
        if (reservation == null || "RELEASED".equals(reservation.getStatus())) {
            return StockResult.ok(command, "Stock already released");
        }
        Map<Long, Integer> qty = parse(reservation.getLines());
        if (!qty.isEmpty()) {
            Map<Long, Product> locked = products.lockAllById(qty.keySet()).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
            for (Map.Entry<Long, Integer> entry : qty.entrySet()) {
                Product product = locked.get(entry.getKey());
                if (product == null || product.getStock() == null) {
                    continue;
                }
                product.setStock(product.getStock() + entry.getValue());
                products.save(product);
            }
        }
        reservation.setStatus("RELEASED");
        reservations.save(reservation);
        log.info("Stock released orderNumber={}", command.orderNumber());
        return StockResult.ok(command, "Stock released");
    }

    private static Map<Long, Integer> parse(String lines) {
        Map<Long, Integer> qty = new LinkedHashMap<>();
        if (lines == null || lines.isBlank()) {
            return qty;
        }
        for (String part : lines.split(",")) {
            String[] bits = part.split(":");
            if (bits.length != 2) {
                continue;
            }
            try {
                qty.put(Long.parseLong(bits[0].trim()), Integer.parseInt(bits[1].trim()));
            } catch (NumberFormatException ignored) {
                // skip malformed reservation line
            }
        }
        return qty;
    }
}
