package com.pharmacy.drugstore.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerLogger {
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerLogger.class);

    public CircuitBreakerLogger(CircuitBreakerRegistry registry) {
        registry.getEventPublisher().onEntryAdded(event -> subscribe(event.getAddedEntry()));
        registry.getAllCircuitBreakers().forEach(this::subscribe);
    }

    private void subscribe(CircuitBreaker breaker) {
        breaker.getEventPublisher().onStateTransition(event ->
                log.warn("Circuit breaker {} {}", event.getCircuitBreakerName(), event.getStateTransition()));
    }
}
