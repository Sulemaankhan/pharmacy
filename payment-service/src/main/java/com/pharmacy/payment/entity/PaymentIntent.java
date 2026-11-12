package com.pharmacy.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_intents")
public class PaymentIntent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    private String orderNumber;
    private String transactionRef;
    private String action;
    @Column(nullable = false, length = 16)
    private String status = "PENDING";
    private String eventId;
    private String gatewayRef;
    @Column(length = 500)
    private String message;
    @Column(columnDefinition = "TEXT")
    private String resultJson;
    @Column(columnDefinition = "TEXT")
    private String commandJson;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getGatewayRef() { return gatewayRef; }
    public void setGatewayRef(String gatewayRef) { this.gatewayRef = gatewayRef; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }
    public String getCommandJson() { return commandJson; }
    public void setCommandJson(String commandJson) { this.commandJson = commandJson; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
