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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

    public ShipmentTrackingService(
            ShipmentRepository shipments,
            ShipmentHub hub,
            KafkaTemplate<String, Object> kafka,
            @Value("${app.kafka.topics.shipment-updates}") String updatesTopic) {
        this.shipments = shipments;
        this.hub = hub;
        this.kafka = kafka;
        this.updatesTopic = updatesTopic;
    }

    @Transactional
    public void handle(ShipmentCommand command) {
        if (command == null || command.type() == null) {
            return;
        }
        String type = command.type().trim().toUpperCase();
        if ("CREATE".equals(type)) {
            applyCreate(command);
            return;
        }
        if ("PAYMENT_RESULT".equals(type)) {
            applyPayment(command);
        }
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

    private void applyCreate(ShipmentCommand command) {
        Shipment shipment = find(command);
        if (shipment == null) {
            log.warn("Shipment create ignored missing row tracking={} orderNumber={}",
                    command.trackingNumber(), command.orderNumber());
            return;
        }
        copyDetails(shipment, command);
        if (shipment.getStatus() == null) {
            shipment.setStatus(ShipmentStatus.PENDING);
        }
        publish(save(shipment));
        log.info("Shipment registered tracking={} orderNumber={}",
                shipment.getTrackingNumber(), shipment.getOrderNumber());
    }

    private void applyPayment(ShipmentCommand command) {
        Shipment shipment = find(command);
        if (shipment == null) {
            log.warn("Shipment payment ignored missing row tracking={} orderNumber={}",
                    command.trackingNumber(), command.orderNumber());
            return;
        }
        copyDetails(shipment, command);
        boolean paid = Boolean.TRUE.equals(command.paid());
        if (paid) {
            if (shipment.getStatus() == ShipmentStatus.PENDING || shipment.getStatus() == null) {
                shipment.setStatus(ShipmentStatus.CONFIRMED);
            }
        } else {
            shipment.setStatus(ShipmentStatus.CANCELLED);
        }
        publish(save(shipment));
        log.info("Shipment payment applied tracking={} paid={} status={}",
                shipment.getTrackingNumber(), paid, shipment.getStatus());
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
