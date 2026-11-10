package com.pharmacy.shipment.kafka;

import com.pharmacy.shipment.event.ShipmentCommand;
import com.pharmacy.shipment.event.ShipmentResult;
import com.pharmacy.shipment.service.ShipmentTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class ShipmentCommandListener {
    private static final Logger log = LoggerFactory.getLogger(ShipmentCommandListener.class);
    private final ShipmentTrackingService tracking;

    public ShipmentCommandListener(ShipmentTrackingService tracking) {
        this.tracking = tracking;
    }

    @KafkaListener(topics = "${app.kafka.topics.shipment-commands}", groupId = "shipment-service")
    @SendTo("${app.kafka.topics.shipment-replies}")
    public ShipmentResult onCommand(ShipmentCommand command) {
        if (command == null) {
            log.warn("Shipment command was empty");
            return ShipmentResult.fail(null, "Shipment command was empty");
        }
        log.info("Shipment command received type={} tracking={} orderNumber={}",
                command.type(), command.trackingNumber(), command.orderNumber());
        try {
            ShipmentResult result = tracking.handle(command);
            log.info("Shipment command finished type={} tracking={} success={} message={}",
                    command.type(), command.trackingNumber(), result.success(), result.message());
            return result;
        } catch (Exception ex) {
            log.error("Shipment command failed type={} tracking={}",
                    command.type(), command.trackingNumber(), ex);
            return ShipmentResult.fail(command, ex.getMessage() == null
                    ? "Shipment could not be processed" : ex.getMessage());
        }
    }
}
