package com.pharmacy.drugstore.kafka;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.Shipment;
import com.pharmacy.drugstore.entity.User;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class ShipmentKafkaClient {
    private static final Logger log = LoggerFactory.getLogger(ShipmentKafkaClient.class);
    private final ReplyingKafkaTemplate<String, Object, ShipmentResult> replies;
    private final String commandsTopic;

    public ShipmentKafkaClient(
            ReplyingKafkaTemplate<String, Object, ShipmentResult> shipmentReplyKafkaTemplate,
            @Value("${app.kafka.topics.shipment-commands}") String commandsTopic) {
        this.replies = shipmentReplyKafkaTemplate;
        this.commandsTopic = commandsTopic;
    }

    public ShipmentResult reserve(User user, CustomerOrder order) {
        return sendAndWait(toCommand("RESERVE", user, order, null, UUID.randomUUID().toString()));
    }

    public ShipmentResult cancel(User user, CustomerOrder order) {
        return sendAndWait(toCommand("CANCEL", user, order, false, UUID.randomUUID().toString()));
    }

    public void paymentResult(User user, CustomerOrder order, boolean paid) {
        sendAndWait(toCommand("PAYMENT_RESULT", user, order, paid, UUID.randomUUID().toString()));
    }

    private ShipmentResult sendAndWait(ShipmentCommand command) {
        if (command == null || command.trackingNumber() == null) {
            return new ShipmentResult(null, null, null, false, "UNAVAILABLE", "Shipment details are missing");
        }
        log.info("Shipment command published type={} tracking={} topic={}",
                command.type(), command.trackingNumber(), commandsTopic);
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(commandsTopic, command.trackingNumber(), command);
        try {
            RequestReplyFuture<String, Object, ShipmentResult> future = replies.sendAndReceive(record);
            ShipmentResult result = future.get(20, TimeUnit.SECONDS).value();
            if (result == null) {
                return new ShipmentResult(command.correlationId(), command.orderNumber(), command.trackingNumber(),
                        false, "UNAVAILABLE", "Shipment service returned an empty result");
            }
            log.info("Shipment result received tracking={} success={} message={}",
                    command.trackingNumber(), result.success(), result.message());
            return result;
        } catch (Exception ex) {
            log.error("Shipment kafka request failed tracking={}", command.trackingNumber(), ex);
            return new ShipmentResult(command.correlationId(), command.orderNumber(), command.trackingNumber(),
                    false, "UNAVAILABLE", "Shipment is not available. The shipment service did not respond.");
        }
    }

    private static ShipmentCommand toCommand(String type, User user, CustomerOrder order, Boolean paid, String correlationId) {
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
                shipment.getCreatedAt(),
                correlationId);
    }
}
