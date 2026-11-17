package com.pharmacy.cloudgateway;

import com.pharmacy.cloudgateway.ratelimit.RateLimitProperties;
import com.pharmacy.cloudgateway.throttle.ThrottleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({RateLimitProperties.class, ThrottleProperties.class})
public class CloudGatewayApplication {
    private static final Logger log = LoggerFactory.getLogger(CloudGatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CloudGatewayApplication.class, args);
        log.info("API Cloud gateway started (reactive)");
    }
}
