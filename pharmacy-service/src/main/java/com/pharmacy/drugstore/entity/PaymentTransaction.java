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

    @Column(precision = 12, scale = 2)
    private BigDecimal debitedAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal availableBalance;

    @Column(precision = 12, scale = 2)
    private BigDecimal creditedAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal pharmacyBalance;

    private String maskedInstrument;
    private String gatewayMessage;
    private boolean ledgerPosted = false;
    private boolean ledgerReversed = false;
    private boolean refunded = false;

    @Column(precision = 12, scale = 2)
    private BigDecimal refundedAmount;

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
    public BigDecimal getDebitedAmount() { return debitedAmount; }
    public void setDebitedAmount(BigDecimal debitedAmount) { this.debitedAmount = debitedAmount; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    public BigDecimal getCreditedAmount() { return creditedAmount; }
    public void setCreditedAmount(BigDecimal creditedAmount) { this.creditedAmount = creditedAmount; }
    public BigDecimal getPharmacyBalance() { return pharmacyBalance; }
    public void setPharmacyBalance(BigDecimal pharmacyBalance) { this.pharmacyBalance = pharmacyBalance; }
    public String getMaskedInstrument() { return maskedInstrument; }
    public void setMaskedInstrument(String maskedInstrument) { this.maskedInstrument = maskedInstrument; }
    public String getGatewayMessage() { return gatewayMessage; }
    public void setGatewayMessage(String gatewayMessage) { this.gatewayMessage = gatewayMessage; }
    public boolean isLedgerPosted() { return ledgerPosted; }
    public void setLedgerPosted(boolean ledgerPosted) { this.ledgerPosted = ledgerPosted; }
    public boolean isLedgerReversed() { return ledgerReversed; }
    public void setLedgerReversed(boolean ledgerReversed) { this.ledgerReversed = ledgerReversed; }
    public boolean isRefunded() { return refunded; }
    public void setRefunded(boolean refunded) { this.refunded = refunded; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) { this.refundedAmount = refundedAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public java.util.List<LedgerEntry> getLedgerEntries() { return ledgerEntries; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
}
