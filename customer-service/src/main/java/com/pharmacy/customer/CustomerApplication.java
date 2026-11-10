package com.pharmacy.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class CustomerApplication {
    private static final Logger log = LoggerFactory.getLogger(CustomerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
        log.info("Pharmacy customer service started");
    }
}
