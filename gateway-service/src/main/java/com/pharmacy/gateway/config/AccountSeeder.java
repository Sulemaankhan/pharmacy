package com.pharmacy.gateway.config;

import com.pharmacy.gateway.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountSeeder {
    private static final Logger log = LoggerFactory.getLogger(AccountSeeder.class);

    @Bean
    CommandLineRunner seedDemoAccount(AccountService accounts) {
        return args -> {
            var pharmacy = accounts.pharmacySnapshot();
            log.info("Pharmacy account ready email={}", pharmacy.email());
            var snapshot = accounts.snapshot("demo@pharmacy.com", null, "Demo User");
            log.info("Demo account ready email={}", snapshot.email());
        };
    }
}
