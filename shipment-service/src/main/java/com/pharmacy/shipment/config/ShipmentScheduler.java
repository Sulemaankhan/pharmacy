package com.pharmacy.shipment.config;

import com.pharmacy.shipment.service.ShipmentTrackingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShipmentScheduler {
    private final ShipmentTrackingService tracking;

    public ShipmentScheduler(ShipmentTrackingService tracking) {
        this.tracking = tracking;
    }

    @Scheduled(fixedDelay = 10000)
    public void tick() {
        tracking.refreshActive();
    }
}
