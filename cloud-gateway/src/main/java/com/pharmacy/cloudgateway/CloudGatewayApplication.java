package com.pharmacy.cloudgateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CloudGatewayApplication {
    private static final Logger log = LoggerFactory.getLogger(CloudGatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CloudGatewayApplication.class, args);
        log.info("Pharmacy cloud gateway started (reactive)");
    }
}
