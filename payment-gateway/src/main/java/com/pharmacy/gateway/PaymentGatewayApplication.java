package com.pharmacy.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PaymentGatewayApplication {
    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
        log.info("Pharmacy payment gateway started");
    }
}
