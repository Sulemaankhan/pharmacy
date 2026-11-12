package com.pharmacy.drugstore.kafka;

import com.pharmacy.drugstore.dto.AccountSnapshot;
import com.pharmacy.drugstore.dto.GatewayDebitRequest;
import com.pharmacy.drugstore.dto.GatewayDebitResponse;
import com.pharmacy.drugstore.resilience.OutboundRest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PaymentGatewayClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);
    private static final String UNAVAILABLE = "Gateway service is unavailable. Start gateway-service on port 8086.";
    private final RestClient rest;

    public PaymentGatewayClient(@Value("${app.gateway-service.base-url:http://localhost:8086}") String baseUrl) {
        this.rest = OutboundRest.client(baseUrl);
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "accountFallback")
    public AccountSnapshot getAccount(String email, Long userId, String customerName) {
        AccountSnapshot snapshot = rest.get()
                .uri(uri -> {
                    var builder = uri.path("/internal/accounts").queryParam("email", email);
                    if (userId != null) {
                        builder.queryParam("userId", userId);
                    }
                    if (customerName != null && !customerName.isBlank()) {
                        builder.queryParam("customerName", customerName);
                    }
                    return builder.build();
                })
                .retrieve()
                .body(AccountSnapshot.class);
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment gateway returned an empty account");
        }
        return snapshot;
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "refundFallback")
    public GatewayDebitResponse refund(GatewayDebitRequest request) {
        try {
            GatewayDebitResponse response = rest.post()
                    .uri("/internal/accounts/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GatewayDebitResponse.class);
            if (response == null) {
                return GatewayDebitResponse.failed("Payment gateway returned an empty refund result");
            }
            return response;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().is4xxClientError()) {
                return GatewayDebitResponse.failed(ex.getStatusText());
            }
            throw ex;
        }
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "pharmacyFallback")
    public AccountSnapshot getPharmacyAccount() {
        AccountSnapshot snapshot = rest.get()
                .uri("/internal/accounts/pharmacy")
                .retrieve()
                .body(AccountSnapshot.class);
        if (snapshot == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment gateway returned an empty pharmacy account");
        }
        return snapshot;
    }

    private AccountSnapshot accountFallback(String email, Long userId, String customerName, Throwable ex) {
        if (ex instanceof ResponseStatusException status) {
            throw status;
        }
        log.warn("Payment gateway account lookup failed email={} reason={}", email, OutboundRest.reason(ex));
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }

    private GatewayDebitResponse refundFallback(GatewayDebitRequest request, Throwable ex) {
        log.warn("Payment gateway refund failed order={} reason={}",
                request == null ? null : request.orderNumber(), OutboundRest.reason(ex));
        return GatewayDebitResponse.failed(UNAVAILABLE);
    }

    private AccountSnapshot pharmacyFallback(Throwable ex) {
        if (ex instanceof ResponseStatusException status) {
            throw status;
        }
        log.warn("Payment gateway pharmacy account lookup failed reason={}", OutboundRest.reason(ex));
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNAVAILABLE);
    }
}
