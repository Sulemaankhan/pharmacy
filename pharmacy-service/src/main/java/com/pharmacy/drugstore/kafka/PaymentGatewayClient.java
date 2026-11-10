package com.pharmacy.drugstore.kafka;

import com.pharmacy.drugstore.dto.AccountSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PaymentGatewayClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);
    private final RestClient rest;

    public PaymentGatewayClient(@Value("${app.payment-gateway.base-url:http://localhost:8086}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    public AccountSnapshot getAccount(String email, Long userId, String customerName) {
        try {
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
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("Payment gateway account lookup failed email={}", email, ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment gateway is unavailable. Start payment-gateway on port 8086.");
        }
    }

    public AccountSnapshot getPharmacyAccount() {
        try {
            AccountSnapshot snapshot = rest.get()
                    .uri("/internal/accounts/pharmacy")
                    .retrieve()
                    .body(AccountSnapshot.class);
            if (snapshot == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment gateway returned an empty pharmacy account");
            }
            return snapshot;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("Payment gateway pharmacy account lookup failed", ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment gateway is unavailable. Start payment-gateway on port 8086.");
        }
    }
}
