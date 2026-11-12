package com.pharmacy.drugstore.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "checkout_sagas")
public class CheckoutSaga {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    private Long userId;
    private String customerEmail;
    private String customerName;
    private String transactionRef;
    private String paymentMode;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String status = "RUNNING";

    @Column(nullable = false)
    private String step = "STOCK";

    private boolean stockReserved;
    private boolean paymentCaptured;
    private boolean pharmacyReady;
    private boolean shipmentReserved;
    private boolean delivered;
    private boolean compensated;

    @Column(columnDefinition = "TEXT")
    private String paymentCommandJson;

    @Column(columnDefinition = "TEXT")
    private String shipmentCommandJson;

    @Column(length = 64)
    private String lastEventId;

    @Column(length = 500)
    private String failureReason;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant stepStartedAt = Instant.now();

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getStep() { return step; }
    public void setStep(String step) { this.step = step; }
    public boolean isStockReserved() { return stockReserved; }
    public void setStockReserved(boolean stockReserved) { this.stockReserved = stockReserved; }
    public boolean isPaymentCaptured() { return paymentCaptured; }
    public void setPaymentCaptured(boolean paymentCaptured) { this.paymentCaptured = paymentCaptured; }
    public boolean isPharmacyReady() { return pharmacyReady; }
    public void setPharmacyReady(boolean pharmacyReady) { this.pharmacyReady = pharmacyReady; }
    public boolean isShipmentReserved() { return shipmentReserved; }
    public void setShipmentReserved(boolean shipmentReserved) { this.shipmentReserved = shipmentReserved; }
    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }
    public boolean isCompensated() { return compensated; }
    public void setCompensated(boolean compensated) { this.compensated = compensated; }
    public String getPaymentCommandJson() { return paymentCommandJson; }
    public void setPaymentCommandJson(String paymentCommandJson) { this.paymentCommandJson = paymentCommandJson; }
    public String getShipmentCommandJson() { return shipmentCommandJson; }
    public void setShipmentCommandJson(String shipmentCommandJson) { this.shipmentCommandJson = shipmentCommandJson; }
    public String getLastEventId() { return lastEventId; }
    public void setLastEventId(String lastEventId) { this.lastEventId = lastEventId; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getStepStartedAt() { return stepStartedAt; }
    public void setStepStartedAt(Instant stepStartedAt) { this.stepStartedAt = stepStartedAt; }
}
