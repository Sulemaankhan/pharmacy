package com.pharmacy.drugstore.payment.strategy;

import com.pharmacy.drugstore.payment.PaymentMode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentStrategyRegistry {
    private final Map<PaymentMode, PaymentStrategy> strategies = new EnumMap<>(PaymentMode.class);

    public PaymentStrategyRegistry(List<PaymentStrategy> implementations) {
        for (PaymentStrategy strategy : implementations) {
            strategies.put(strategy.mode(), strategy);
        }
    }

    public PaymentStrategy of(PaymentMode mode) {
        PaymentStrategy strategy = strategies.get(mode);
        if (strategy == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment mode: " + mode);
        }
        return strategy;
    }
}
