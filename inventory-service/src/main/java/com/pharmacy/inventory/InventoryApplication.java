package com.pharmacy.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableScheduling
public class InventoryApplication {
    private static final Logger log = LoggerFactory.getLogger(InventoryApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
        log.info("Pharmacy inventory service started");
    }
}
