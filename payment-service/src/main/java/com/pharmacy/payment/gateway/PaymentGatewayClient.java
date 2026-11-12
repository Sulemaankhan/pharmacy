package com.pharmacy.payment.gateway;

import com.pharmacy.payment.resilience.OutboundRest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PaymentGatewayClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);
    public static final String UNAVAILABLE = "Payment gateway is unavailable. Please try again.";
    private final RestClient rest;

    public PaymentGatewayClient(@Value("${app.gateway-service.base-url:http://localhost:8086}") String baseUrl) {
        this.rest = OutboundRest.client(baseUrl);
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "refundFallback")
    public DebitResponse refund(DebitRequest request) {
        try {
            DebitResponse response = rest.post()
                    .uri("/internal/accounts/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DebitResponse.class);
            if (response == null) {
                throw new IllegalStateException("Payment gateway returned an empty refund result");
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return failed(ex.getStatusText());
            }
            throw ex;
        }
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "lookupFallback")
    public DebitResponse lookup(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) {
            return failed("Transaction ref is required");
        }
        try {
            DebitResponse response = rest.get()
                    .uri("/internal/accounts/transactions/{transactionRef}", transactionRef)
                    .retrieve()
                    .body(DebitResponse.class);
            if (response == null) {
                throw new IllegalStateException("Payment gateway returned an empty lookup result");
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return failed(ex.getStatusText());
            }
            throw ex;
        }
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "debitFallback")
    public DebitResponse debit(DebitRequest request) {
        try {
            DebitResponse response = rest.post()
                    .uri("/internal/accounts/debit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DebitResponse.class);
            if (response == null) {
                throw new IllegalStateException("Payment gateway returned an empty result");
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return failed(ex.getStatusText());
            }
            throw ex;
        }
    }

    private DebitResponse debitFallback(DebitRequest request, Throwable ex) {
        return fallback("debit", request == null ? null : request.orderNumber(), ex);
    }

    private DebitResponse refundFallback(DebitRequest request, Throwable ex) {
        return fallback("refund", request == null ? null : request.orderNumber(), ex);
    }

    private DebitResponse lookupFallback(String transactionRef, Throwable ex) {
        return fallback("lookup", transactionRef, ex);
    }

    private DebitResponse fallback(String action, String key, Throwable ex) {
        if (ex instanceof RestClientResponseException response && response.getStatusCode().is4xxClientError()) {
            return failed(response.getStatusText());
        }
        if (OutboundRest.open(ex)) {
            log.warn("Payment gateway {} skipped, circuit open key={}", action, key);
        } else {
            log.warn("Payment gateway {} failed key={} reason={}", action, key, OutboundRest.reason(ex));
        }
        return failed(UNAVAILABLE);
    }

    private static DebitResponse failed(String message) {
        return new DebitResponse(false, message, null, null, null, null, null);
    }
}
