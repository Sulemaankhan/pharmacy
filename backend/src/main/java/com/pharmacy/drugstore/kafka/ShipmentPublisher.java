package com.pharmacy.drugstore.kafka;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.Shipment;
import com.pharmacy.drugstore.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShipmentPublisher {
    private static final Logger log = LoggerFactory.getLogger(ShipmentPublisher.class);
    private final KafkaTemplate<String, Object> kafka;
    private final String topic;

    public ShipmentPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${app.kafka.topics.shipment-commands}") String topic) {
        this.kafka = kafkaTemplate;
        this.topic = topic;
    }

    public void created(User user, CustomerOrder order) {
        publish(toCommand("CREATE", user, order, null));
    }

    public void paymentResult(User user, CustomerOrder order, boolean paid) {
        publish(toCommand("PAYMENT_RESULT", user, order, paid));
    }

    private void publish(ShipmentCommand command) {
        if (command == null || command.trackingNumber() == null) {
            return;
        }
        log.info("Shipment command published type={} tracking={} topic={}",
                command.type(), command.trackingNumber(), topic);
        try {
            kafka.send(topic, command.trackingNumber(), command);
        } catch (Exception ex) {
            log.error("Shipment kafka publish failed type={} tracking={}",
                    command.type(), command.trackingNumber(), ex);
        }
    }

    private static ShipmentCommand toCommand(String type, User user, CustomerOrder order, Boolean paid) {
        Shipment shipment = order == null ? null : order.getShipment();
        if (shipment == null) {
            return null;
        }
        return new ShipmentCommand(
                type,
                user == null ? null : user.getId(),
                user == null ? null : user.getEmail(),
                order.getId(),
                order.getOrderNumber(),
                shipment.getTrackingNumber(),
                paid,
                shipment.getRecipientName(),
                shipment.getAddress(),
                shipment.getCity(),
                shipment.getState(),
                shipment.getPincode(),
                shipment.getContactNumber(),
                shipment.getEmail(),
                shipment.getCarrier(),
                shipment.getEstimatedDelivery(),
                shipment.getCreatedAt());
    }
}
