package com.pharmacy.drugstore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id")
    private PaymentTransaction transaction;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id")
    private LedgerAccount account;

    @Column(nullable = false, length = 16)
    private String entryType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public PaymentTransaction getTransaction() { return transaction; }
    public void setTransaction(PaymentTransaction transaction) { this.transaction = transaction; }
    public LedgerAccount getAccount() { return account; }
    public void setAccount(LedgerAccount account) { this.account = account; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Instant getCreatedAt() { return createdAt; }
}
