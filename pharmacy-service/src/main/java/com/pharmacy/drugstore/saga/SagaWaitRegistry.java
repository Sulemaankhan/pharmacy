package com.pharmacy.drugstore.saga;

import com.pharmacy.drugstore.dto.PaymentResponse;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class SagaWaitRegistry {
    private final ConcurrentHashMap<String, CompletableFuture<PaymentResponse>> waits = new ConcurrentHashMap<>();

    public CompletableFuture<PaymentResponse> register(String orderNumber) {
        CompletableFuture<PaymentResponse> future = new CompletableFuture<>();
        waits.put(orderNumber, future);
        return future;
    }

    public void complete(String orderNumber, PaymentResponse response) {
        CompletableFuture<PaymentResponse> future = waits.remove(orderNumber);
        if (future != null) {
            future.complete(response);
        }
    }

    public PaymentResponse await(String orderNumber, Duration timeout) {
        CompletableFuture<PaymentResponse> future = waits.get(orderNumber);
        if (future == null) {
            return null;
        }
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            waits.remove(orderNumber, future);
            return null;
        }
    }
}
