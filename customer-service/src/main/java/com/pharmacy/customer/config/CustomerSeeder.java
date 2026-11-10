package com.pharmacy.customer.config;

import com.pharmacy.customer.entity.User;
import com.pharmacy.customer.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CustomerSeeder {
    private static final Logger log = LoggerFactory.getLogger(CustomerSeeder.class);

    @Bean
    CommandLineRunner seedDemoUser(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) {
                return;
            }
            User demo = new User();
            demo.setName("Demo User");
            demo.setEmail("demo@pharmacy.com");
            demo.setPassword(encoder.encode("demo123"));
            demo.setRole("USER");
            users.save(demo);
            log.info("Seeded demo customer email={}", demo.getEmail());
        };
    }
}
