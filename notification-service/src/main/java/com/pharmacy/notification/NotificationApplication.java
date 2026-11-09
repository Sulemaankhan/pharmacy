package com.pharmacy.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class NotificationApplication {
    private static final Logger log = LoggerFactory.getLogger(NotificationApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
        log.info("Pharmacy notification Kafka service started");
    }
}
