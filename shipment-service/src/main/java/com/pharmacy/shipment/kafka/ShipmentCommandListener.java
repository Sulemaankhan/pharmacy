package com.pharmacy.shipment.kafka;

import com.pharmacy.shipment.event.ShipmentCommand;
import com.pharmacy.shipment.service.ShipmentTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ShipmentCommandListener {
    private static final Logger log = LoggerFactory.getLogger(ShipmentCommandListener.class);
    private final ShipmentTrackingService tracking;

    public ShipmentCommandListener(ShipmentTrackingService tracking) {
        this.tracking = tracking;
    }

    @KafkaListener(topics = "${app.kafka.topics.shipment-commands}", groupId = "shipment-service")
    public void onCommand(ShipmentCommand command) {
        if (command == null) {
            log.warn("Shipment command was empty");
            return;
        }
        log.info("Shipment command received type={} tracking={} orderNumber={}",
                command.type(), command.trackingNumber(), command.orderNumber());
        try {
            tracking.handle(command);
        } catch (Exception ex) {
            log.error("Shipment command failed type={} tracking={}",
                    command.type(), command.trackingNumber(), ex);
        }
    }
}
