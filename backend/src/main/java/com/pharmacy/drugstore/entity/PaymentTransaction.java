package com.pharmacy.drugstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pharmacy.drugstore.payment.PaymentMode;
import com.pharmacy.drugstore.payment.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transactionRef;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    private String maskedInstrument;
    private String gatewayMessage;
    private boolean ledgerPosted = false;
    private Instant createdAt = Instant.now();
    private Instant completedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "transaction")
    private java.util.List<LedgerEntry> ledgerEntries = new java.util.ArrayList<>();

    @JsonIgnore
    @OneToOne(optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    public Long getId() { return id; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public void setPaymentMode(PaymentMode paymentMode) { this.paymentMode = paymentMode; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMaskedInstrument() { return maskedInstrument; }
    public void setMaskedInstrument(String maskedInstrument) { this.maskedInstrument = maskedInstrument; }
    public String getGatewayMessage() { return gatewayMessage; }
    public void setGatewayMessage(String gatewayMessage) { this.gatewayMessage = gatewayMessage; }
    public boolean isLedgerPosted() { return ledgerPosted; }
    public void setLedgerPosted(boolean ledgerPosted) { this.ledgerPosted = ledgerPosted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public java.util.List<LedgerEntry> getLedgerEntries() { return ledgerEntries; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
}
