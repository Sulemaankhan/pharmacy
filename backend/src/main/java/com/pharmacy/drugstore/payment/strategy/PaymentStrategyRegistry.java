package com.pharmacy.drugstore.payment.strategy;

import com.pharmacy.drugstore.payment.PaymentMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentStrategyRegistry {
    private static final Logger log = LoggerFactory.getLogger(PaymentStrategyRegistry.class);
    private final Map<PaymentMode, PaymentStrategy> strategies = new EnumMap<>(PaymentMode.class);

    public PaymentStrategyRegistry(List<PaymentStrategy> implementations) {
        for (PaymentStrategy strategy : implementations) {
            strategies.put(strategy.mode(), strategy);
        }
    }

    public PaymentStrategy of(PaymentMode mode) {
        PaymentStrategy strategy = strategies.get(mode);
        if (strategy == null) {
            log.warn("Payment strategy missing mode={}", mode);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment mode: " + mode);
        }
        log.info("Payment strategy selected mode={}", mode);
        return strategy;
    }
}
