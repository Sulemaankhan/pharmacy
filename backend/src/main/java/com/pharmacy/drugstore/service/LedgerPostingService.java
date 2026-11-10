package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.entity.LedgerAccount;
import com.pharmacy.drugstore.entity.LedgerEntry;
import com.pharmacy.drugstore.entity.PaymentTransaction;
import com.pharmacy.drugstore.payment.PaymentMode;
import com.pharmacy.drugstore.repository.LedgerAccountRepository;
import com.pharmacy.drugstore.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

@Service
public class LedgerPostingService {
    private static final Logger log = LoggerFactory.getLogger(LedgerPostingService.class);
    private final LedgerAccountRepository accounts;
    private final LedgerEntryRepository entries;

    public LedgerPostingService(LedgerAccountRepository accounts, LedgerEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    public void post(PaymentTransaction txn) {
        if (txn.isLedgerPosted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already posted to ledger");
        }
        BigDecimal amount = txn.getAmount();
        if (amount == null || amount.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment amount must be greater than zero");
        }

        LedgerAccount debit = lock(clearingAccount(txn.getPaymentMode()));
        LedgerAccount credit = lockOrCreate("PHARMACY_WALLET", "Pharmacy account", "ASSET");

        addEntry(txn, debit, "DEBIT", amount);
        addEntry(txn, credit, "CREDIT", amount);

        debit.setBalance(debit.getBalance().add(amount));
        credit.setBalance(credit.getBalance().add(amount));
        accounts.save(debit);
        accounts.save(credit);
        txn.setLedgerPosted(true);
        txn.setLedgerReversed(false);
        log.info("Ledger posted txn={} amount={} debit={} credit={}",
                txn.getTransactionRef(), amount, debit.getCode(), credit.getCode());
    }

    public void reverse(PaymentTransaction txn) {
        if (txn == null || !txn.isLedgerPosted() || txn.isLedgerReversed()) {
            return;
        }
        BigDecimal amount = txn.getAmount();
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        LedgerAccount debit = lock(clearingAccount(txn.getPaymentMode()));
        LedgerAccount credit = lockOrCreate("PHARMACY_WALLET", "Pharmacy account", "ASSET");
        addEntry(txn, credit, "DEBIT", amount);
        addEntry(txn, debit, "CREDIT", amount);
        debit.setBalance(debit.getBalance().subtract(amount));
        credit.setBalance(credit.getBalance().subtract(amount));
        accounts.save(debit);
        accounts.save(credit);
        txn.setLedgerPosted(false);
        txn.setLedgerReversed(true);
        log.info("Ledger reversed txn={} amount={}", txn.getTransactionRef(), amount);
    }

    private void addEntry(PaymentTransaction txn, LedgerAccount account, String type, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setTransaction(txn);
        entry.setAccount(account);
        entry.setEntryType(type);
        entry.setAmount(amount);
        txn.getLedgerEntries().add(entry);
        entries.save(entry);
    }

    private LedgerAccount lock(String code) {
        return accounts.lockByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Ledger account missing: " + code));
    }

    private LedgerAccount lockOrCreate(String code, String name, String type) {
        return accounts.lockByCode(code).orElseGet(() -> {
            LedgerAccount account = new LedgerAccount();
            account.setCode(code);
            account.setName(name);
            account.setType(type);
            account.setBalance(BigDecimal.ZERO);
            accounts.saveAndFlush(account);
            return accounts.lockByCode(code).orElse(account);
        });
    }

    private String clearingAccount(PaymentMode mode) {
        return switch (mode) {
            case UPI -> "UPI_CLEARING";
            case CARD -> "CARD_CLEARING";
            case NET_BANKING -> "NETBANKING_CLEARING";
            case WALLET -> "WALLET_CLEARING";
            case COD -> "COD_RECEIVABLE";
        };
    }
}
