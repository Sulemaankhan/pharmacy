package com.pharmacy.drugstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PharmacyApplication {
    private static final Logger log = LoggerFactory.getLogger(PharmacyApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(PharmacyApplication.class, args);
        log.info("Medicine Drugstore API started");
    }
}
