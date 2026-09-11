package com.pharmacy.drugstore.config;

import com.pharmacy.drugstore.service.ShipmentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShipmentScheduler {
    private final ShipmentService shipmentService;

    public ShipmentScheduler(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @Scheduled(fixedDelay = 10000)
    public void tick() {
        shipmentService.refreshActive();
    }
}
