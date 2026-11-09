package com.pharmacy.shipment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
public class ShipmentApplication {
    private static final Logger log = LoggerFactory.getLogger(ShipmentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ShipmentApplication.class, args);
        log.info("Pharmacy shipment Kafka service started");
    }
}
