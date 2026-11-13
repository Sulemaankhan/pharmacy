package com.pharmacy.inventory.service;

import com.pharmacy.inventory.entity.Product;
import com.pharmacy.inventory.entity.StockReservation;
import com.pharmacy.inventory.event.DomainEvent;
import com.pharmacy.inventory.event.StockCommand;
import com.pharmacy.inventory.event.StockResult;
import com.pharmacy.inventory.reliability.OutboxService;
import com.pharmacy.inventory.repository.ProductRepository;
import com.pharmacy.inventory.repository.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockService {
    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private final ProductRepository products;
    private final StockReservationRepository reservations;
    private final OutboxService outbox;
    private final String inventoryEvents;

    public StockService(
            ProductRepository products,
            StockReservationRepository reservations,
            OutboxService outbox,
            @Value("${app.kafka.topics.inventory-events}") String inventoryEvents) {
        this.products = products;
        this.reservations = reservations;
        this.outbox = outbox;
        this.inventoryEvents = inventoryEvents;
    }

    @Transactional
    public StockResult handle(StockCommand command) {
        if (command == null || command.action() == null) {
            return StockResult.fail(command, "Stock command was empty");
        }
        String action = command.action().trim().toUpperCase();
        StockResult result;
        if ("RESERVE".equals(action)) {
            result = reserve(command);
        } else if ("RELEASE".equals(action)) {
            result = release(command);
        } else {
            result = StockResult.fail(command, "Unsupported stock action " + action);
        }
        if ("RESERVE".equals(action)) {
            publish(command, result.success() ? "INVENTORY_RESERVED" : "INVENTORY_FAILED",
                    result.success(), result.message());
        } else if ("RELEASE".equals(action) && result.success()) {
            publish(command, "INVENTORY_RELEASED", true, result.message());
        }
        return result;
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
        Map<Long, Product> found = products.findAllById(qty.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        for (Long productId : qty.keySet()) {
            if (!found.containsKey(productId)) {
                return StockResult.fail(command, "Product not found: " + productId);
            }
        }
        List<Long> taken = new ArrayList<>();
        for (Long productId : qty.keySet().stream().sorted().toList()) {
            int wanted = qty.get(productId);
            if (products.decrementIfAvailable(productId, wanted) != 1) {
                restore(taken, qty);
                return StockResult.fail(command, "Insufficient stock for " + found.get(productId).getName());
            }
            taken.add(productId);
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
        restore(qty.keySet().stream().sorted().toList(), qty);
        reservation.setStatus("RELEASED");
        reservations.save(reservation);
        log.info("Stock released orderNumber={}", command.orderNumber());
        return StockResult.ok(command, "Stock released");
    }

    private void publish(StockCommand command, String eventType, boolean success, String message) {
        if (command == null || command.orderNumber() == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", success);
        payload.put("message", message);
        payload.put("action", command.action());
        DomainEvent event = DomainEvent.of(eventType, command.orderNumber(), command.correlationId(), payload);
        outbox.enqueue(inventoryEvents, command.orderNumber(), event);
    }

    private void restore(List<Long> productIds, Map<Long, Integer> qty) {
        for (Long productId : productIds) {
            Integer amount = qty.get(productId);
            if (productId == null || amount == null || amount < 1) {
                continue;
            }
            products.incrementStock(productId, amount);
        }
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
