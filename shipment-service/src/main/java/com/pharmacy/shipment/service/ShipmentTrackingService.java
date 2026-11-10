package com.pharmacy.shipment.service;

import com.pharmacy.shipment.entity.Shipment;
import com.pharmacy.shipment.event.ShipmentCommand;
import com.pharmacy.shipment.event.ShipmentUpdate;
import com.pharmacy.shipment.mapper.ShipmentMapper;
import com.pharmacy.shipment.realtime.ShipmentHub;
import com.pharmacy.shipment.repository.ShipmentRepository;
import com.pharmacy.shipment.shipment.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pharmacy.shipment.event.ShipmentResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShipmentTrackingService {
    private static final Logger log = LoggerFactory.getLogger(ShipmentTrackingService.class);
    private static final List<ShipmentStatus> PIPELINE = List.of(
            ShipmentStatus.CONFIRMED,
            ShipmentStatus.PACKED,
            ShipmentStatus.SHIPPED,
            ShipmentStatus.OUT_FOR_DELIVERY,
            ShipmentStatus.DELIVERED);
    private static final long[] THRESHOLDS_SECONDS = {0, 25, 55, 90, 140};

    private final ShipmentRepository shipments;
    private final ShipmentHub hub;
    private final KafkaTemplate<String, Object> kafka;
    private final String updatesTopic;
    private final Set<String> unserviceablePincodes;

    public ShipmentTrackingService(
            ShipmentRepository shipments,
            ShipmentHub hub,
            KafkaTemplate<String, Object> kafka,
            @Value("${app.kafka.topics.shipment-updates}") String updatesTopic,
            @Value("${app.shipment.unserviceable-pincodes:999999,000000}") String unserviceablePincodes) {
        this.shipments = shipments;
        this.hub = hub;
        this.kafka = kafka;
        this.updatesTopic = updatesTopic;
        this.unserviceablePincodes = Arrays.stream(unserviceablePincodes.split(","))
                .map(String::trim)
                .filter(code -> !code.isBlank())
                .collect(Collectors.toSet());
    }

    @Transactional
    public ShipmentResult handle(ShipmentCommand command) {
        if (command == null || command.type() == null) {
            return ShipmentResult.fail(command, "Shipment command was empty");
        }
        String type = command.type().trim().toUpperCase();
        return switch (type) {
            case "CREATE", "RESERVE" -> applyReserve(command);
            case "CANCEL" -> applyCancel(command);
            case "PAYMENT_RESULT" -> applyPayment(command);
            default -> ShipmentResult.fail(command, "Unsupported shipment command " + type);
        };
    }

    @Transactional
    public void refreshActive() {
        List<Shipment> active = shipments.findByStatusIn(List.of(
                ShipmentStatus.PENDING,
                ShipmentStatus.CONFIRMED,
                ShipmentStatus.PACKED,
                ShipmentStatus.SHIPPED,
                ShipmentStatus.OUT_FOR_DELIVERY));
        active.forEach(this::advance);
    }

    private ShipmentResult applyReserve(ShipmentCommand command) {
        if (!serviceable(command.pincode())) {
            Shipment shipment = findOrCreate(command);
            shipment.setStatus(ShipmentStatus.CANCELLED);
            copyDetails(shipment, command);
            publish(save(shipment));
            String message = "Shipment not available for pincode " + command.pincode();
            log.warn("Shipment unavailable tracking={} pincode={}", command.trackingNumber(), command.pincode());
            return ShipmentResult.fail(command, message);
        }
        Shipment shipment = findOrCreate(command);
        if (shipment.getStatus() == ShipmentStatus.CANCELLED) {
            copyDetails(shipment, command);
            shipment.setStatus(ShipmentStatus.PENDING);
        }
        copyDetails(shipment, command);
        if (shipment.getStatus() == null || shipment.getStatus() == ShipmentStatus.PENDING) {
            shipment.setStatus(ShipmentStatus.PENDING);
        }
        publish(save(shipment));
        log.info("Shipment reserved tracking={} orderNumber={}",
                shipment.getTrackingNumber(), shipment.getOrderNumber());
        return ShipmentResult.ok(command, shipment.getStatus().name(), "Shipment reserved");
    }

    private ShipmentResult applyCancel(ShipmentCommand command) {
        Shipment shipment = findOrCreate(command);
        copyDetails(shipment, command);
        shipment.setStatus(ShipmentStatus.CANCELLED);
        publish(save(shipment));
        log.info("Shipment cancelled tracking={} orderNumber={}",
                shipment.getTrackingNumber(), shipment.getOrderNumber());
        return ShipmentResult.ok(command, ShipmentStatus.CANCELLED.name(), "Shipment cancelled");
    }

    private ShipmentResult applyPayment(ShipmentCommand command) {
        Shipment shipment = findOrCreate(command);
        copyDetails(shipment, command);
        boolean paid = Boolean.TRUE.equals(command.paid());
        if (paid) {
            if (shipment.getStatus() == ShipmentStatus.PENDING || shipment.getStatus() == null
                    || shipment.getStatus() == ShipmentStatus.CANCELLED) {
                shipment.setStatus(ShipmentStatus.CONFIRMED);
            }
        } else {
            shipment.setStatus(ShipmentStatus.CANCELLED);
        }
        publish(save(shipment));
        log.info("Shipment payment applied tracking={} paid={} status={}",
                shipment.getTrackingNumber(), paid, shipment.getStatus());
        return ShipmentResult.ok(command, shipment.getStatus().name(),
                paid ? "Shipment confirmed after payment" : "Shipment cancelled after payment failure");
    }

    private boolean serviceable(String pincode) {
        if (pincode == null || pincode.isBlank() || pincode.trim().length() != 6) {
            return false;
        }
        return !unserviceablePincodes.contains(pincode.trim());
    }

    private Shipment findOrCreate(ShipmentCommand command) {
        Shipment shipment = find(command);
        if (shipment != null) {
            return shipment;
        }
        shipment = new Shipment();
        shipment.setTrackingNumber(command.trackingNumber());
        shipment.setRecipientName(blankToDash(command.recipientName()));
        shipment.setAddress(blankToDash(command.address()));
        shipment.setCity(blankToDash(command.city()));
        shipment.setState(blankToDash(command.state()));
        shipment.setPincode(command.pincode() == null ? "000000" : command.pincode());
        shipment.setContactNumber(blankToDash(command.contactNumber()));
        shipment.setEmail(notBlank(command.email()) ? command.email() : blankToDash(command.customerEmail()));
        shipment.setStatus(ShipmentStatus.PENDING);
        shipment.setOrderId(command.orderId());
        shipment.setUserId(command.userId());
        shipment.setOrderNumber(command.orderNumber());
        shipment.setCreatedAt(command.createdAt() == null ? Instant.now() : command.createdAt());
        return shipment;
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private void advance(Shipment shipment) {
        if (shipment == null || shipment.getStatus() == ShipmentStatus.DELIVERED
                || shipment.getStatus() == ShipmentStatus.CANCELLED) {
            return;
        }
        if (shipment.getStatus() == ShipmentStatus.PENDING) {
            return;
        }
        Instant start = shipment.getCreatedAt() == null ? Instant.now() : shipment.getCreatedAt();
        long elapsed = Math.max(0, Duration.between(start, Instant.now()).getSeconds());
        ShipmentStatus next = PIPELINE.get(0);
        for (int i = THRESHOLDS_SECONDS.length - 1; i >= 0; i--) {
            if (elapsed >= THRESHOLDS_SECONDS[i]) {
                next = PIPELINE.get(i);
                break;
            }
        }
        if (next == shipment.getStatus()) {
            return;
        }
        shipment.setStatus(next);
        if (next == ShipmentStatus.SHIPPED && shipment.getShippedAt() == null) {
            shipment.setShippedAt(Instant.now());
        }
        if (next == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(Instant.now());
        }
        publish(save(shipment));
        log.info("Shipment advanced tracking={} status={}", shipment.getTrackingNumber(), next);
    }

    private Shipment find(ShipmentCommand command) {
        if (command.trackingNumber() != null && !command.trackingNumber().isBlank()) {
            Shipment byTracking = shipments.findByTrackingNumber(command.trackingNumber()).orElse(null);
            if (byTracking != null) {
                return byTracking;
            }
        }
        if (command.orderId() != null) {
            return shipments.findByOrderId(command.orderId()).orElse(null);
        }
        return null;
    }

    private void copyDetails(Shipment shipment, ShipmentCommand command) {
        if (command.userId() != null) {
            shipment.setUserId(command.userId());
        }
        if (command.orderId() != null) {
            shipment.setOrderId(command.orderId());
        }
        if (notBlank(command.orderNumber())) {
            shipment.setOrderNumber(command.orderNumber());
        }
        if (notBlank(command.recipientName())) {
            shipment.setRecipientName(command.recipientName());
        }
        if (notBlank(command.address())) {
            shipment.setAddress(command.address());
        }
        if (notBlank(command.city())) {
            shipment.setCity(command.city());
        }
        if (notBlank(command.state())) {
            shipment.setState(command.state());
        }
        if (notBlank(command.pincode())) {
            shipment.setPincode(command.pincode());
        }
        if (notBlank(command.contactNumber())) {
            shipment.setContactNumber(command.contactNumber());
        }
        if (notBlank(command.email()) || notBlank(command.customerEmail())) {
            shipment.setEmail(notBlank(command.email()) ? command.email() : command.customerEmail());
        }
        if (notBlank(command.carrier())) {
            shipment.setCarrier(command.carrier());
        }
        if (command.estimatedDelivery() != null) {
            shipment.setEstimatedDelivery(command.estimatedDelivery());
        }
        if (command.createdAt() != null && shipment.getCreatedAt() == null) {
            shipment.setCreatedAt(command.createdAt());
        }
    }

    private Shipment save(Shipment shipment) {
        shipment.setUpdatedAt(Instant.now());
        return shipments.saveAndFlush(shipment);
    }

    private void publish(Shipment shipment) {
        ShipmentUpdate update = ShipmentMapper.toUpdate(shipment);
        hub.broadcast(update);
        try {
            kafka.send(updatesTopic, shipment.getTrackingNumber(), update);
        } catch (Exception ex) {
            log.warn("Shipment kafka update failed tracking={}", shipment.getTrackingNumber(), ex);
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
