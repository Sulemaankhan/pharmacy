package com.pharmacy.payment.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PaymentGatewayClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayClient.class);
    private final RestClient rest;

    public PaymentGatewayClient(@Value("${app.gateway-service.base-url:http://localhost:8086}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    public DebitResponse refund(DebitRequest request) {
        try {
            DebitResponse response = rest.post()
                    .uri("/internal/accounts/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DebitResponse.class);
            if (response == null) {
                return new DebitResponse(false, "Payment gateway returned an empty refund result", null, null, null, null, null);
            }
            return response;
        } catch (RestClientException ex) {
            log.error("Payment gateway refund failed order={}", request == null ? null : request.orderNumber(), ex);
            return new DebitResponse(false, "Payment gateway is unavailable for refund. Please try again.", null, null, null, null, null);
        }
    }

    public DebitResponse debit(DebitRequest request) {
        try {
            DebitResponse response = rest.post()
                    .uri("/internal/accounts/debit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DebitResponse.class);
            if (response == null) {
                return new DebitResponse(false, "Payment gateway returned an empty result", null, null, null, null, null);
            }
            return response;
        } catch (RestClientException ex) {
            log.error("Payment gateway debit failed order={}", request == null ? null : request.orderNumber(), ex);
            return new DebitResponse(false, "Payment gateway is unavailable. Please try again.", null, null, null, null, null);
        }
    }
}
