package com.pharmacy.gateway.service;

import com.pharmacy.gateway.dto.AccountSnapshot;
import com.pharmacy.gateway.dto.DebitRequest;
import com.pharmacy.gateway.dto.DebitResponse;
import com.pharmacy.gateway.entity.AccountTransaction;
import com.pharmacy.gateway.entity.CustomerAccount;
import com.pharmacy.gateway.realtime.AccountHub;
import com.pharmacy.gateway.repository.AccountTransactionRepository;
import com.pharmacy.gateway.repository.CustomerAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final String CUSTOMER = "CUSTOMER";
    private static final String MERCHANT = "MERCHANT";

    private final CustomerAccountRepository accounts;
    private final AccountTransactionRepository transactions;
    private final AccountHub hub;
    private final BigDecimal openingBalance;
    private final String pharmacyEmail;
    private final String pharmacyName;

    public AccountService(
            CustomerAccountRepository accounts,
            AccountTransactionRepository transactions,
            AccountHub hub,
            @Value("${app.account.opening-balance:10000}") BigDecimal openingBalance,
            @Value("${app.account.pharmacy-email:pharmacy@pharmacy.com}") String pharmacyEmail,
            @Value("${app.account.pharmacy-name:Pharmacy Account}") String pharmacyName) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.hub = hub;
        this.openingBalance = openingBalance == null ? new BigDecimal("10000") : openingBalance;
        this.pharmacyEmail = normalize(pharmacyEmail);
        this.pharmacyName = pharmacyName == null || pharmacyName.isBlank() ? "Pharmacy Account" : pharmacyName;
    }

    @Transactional
    public AccountSnapshot snapshot(String email, Long userId, String customerName) {
        CustomerAccount account = requireAccount(email, userId, customerName);
        return toSnapshot(account, true);
    }

    @Transactional
    public AccountSnapshot pharmacySnapshot() {
        return toSnapshot(requirePharmacy(), true);
    }

    @Transactional(readOnly = true)
    public DebitResponse lookup(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) {
            return DebitResponse.failed("Transaction ref is required", BigDecimal.ZERO);
        }
        var debit = transactions.findFirstByTransactionRefAndTypeOrderByCreatedAtAsc(transactionRef, "DEBIT");
        if (debit.isEmpty()) {
            return DebitResponse.failed("No debit found for " + transactionRef, BigDecimal.ZERO);
        }
        AccountTransaction row = debit.get();
        CustomerAccount account = row.getAccount();
        CustomerAccount pharmacy = accounts.findByEmailIgnoreCase(pharmacyEmail).orElse(null);
        log.info("Account lookup txn={} amount={}", transactionRef, row.getAmount());
        return DebitResponse.ok(
                transactionRef,
                row.getAmount(),
                account == null ? BigDecimal.ZERO : account.getAvailableBalance(),
                row.getAmount(),
                pharmacy == null ? BigDecimal.ZERO : pharmacy.getAvailableBalance());
    }

    @Transactional
    public DebitResponse debit(DebitRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            return DebitResponse.failed("Customer email is required", BigDecimal.ZERO);
        }
        if (pharmacyEmail.equals(normalize(request.email()))) {
            return DebitResponse.failed("Pharmacy account cannot be charged for a customer purchase", BigDecimal.ZERO);
        }
        BigDecimal amount = request.amount();
        if (amount == null || amount.signum() <= 0) {
            return DebitResponse.failed("Payment amount must be greater than zero", BigDecimal.ZERO);
        }
        CustomerAccount account = lockOrOpen(request.email(), request.userId(), request.customerName());
        if (request.transactionRef() != null && !request.transactionRef().isBlank()) {
            var existing = transactions.findByTransactionRefAndTypeAndAccount(
                    request.transactionRef(), "DEBIT", account);
            if (existing.isPresent()) {
                CustomerAccount pharmacy = creditPharmacy(request, existing.get().getAmount());
                log.info("Account debit idempotent txn={}", request.transactionRef());
                return DebitResponse.ok(
                        request.transactionRef(),
                        existing.get().getAmount(),
                        account.getAvailableBalance(),
                        existing.get().getAmount(),
                        pharmacy.getAvailableBalance());
            }
        }
        BigDecimal available = account.getAvailableBalance() == null ? BigDecimal.ZERO : account.getAvailableBalance();
        if (available.compareTo(amount) < 0) {
            log.warn("Insufficient balance email={} required={}", account.getEmail(), amount);
            return DebitResponse.insufficient(available, amount);
        }

        BigDecimal remaining = available.subtract(amount);
        account.setAvailableBalance(remaining);
        if (request.userId() != null) {
            account.setUserId(request.userId());
        }
        if (request.customerName() != null && !request.customerName().isBlank()) {
            account.setCustomerName(request.customerName());
        }
        if (account.getAccountType() == null || account.getAccountType().isBlank()) {
            account.setAccountType(CUSTOMER);
        }
        account.setUpdatedAt(Instant.now());
        accounts.saveAndFlush(account);

        post(account, "DEBIT", amount, remaining, request,
                "Purchase " + safe(request.orderNumber()) + " · paid to Pharmacy");

        CustomerAccount pharmacy = creditPharmacy(request, amount);
        String gatewayRef = "GW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        hub.broadcast(toSnapshot(account, true));
        hub.broadcast(toSnapshot(pharmacy, true));
        log.info("Account transferred email={} amount={} order={}",
                account.getEmail(), amount, request.orderNumber());
        return DebitResponse.ok(gatewayRef, amount, remaining, amount, pharmacy.getAvailableBalance());
    }

    @Transactional
    public DebitResponse refund(DebitRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            return DebitResponse.failed("Customer email is required", BigDecimal.ZERO);
        }
        CustomerAccount customer = lockOrOpen(request.email(), request.userId(), request.customerName());
        if (request.transactionRef() != null && !request.transactionRef().isBlank()) {
            var existing = transactions.findByTransactionRefAndTypeAndAccount(
                    request.transactionRef(), "CREDIT", customer);
            if (existing.isPresent() && existing.get().getDescription() != null
                    && existing.get().getDescription().toLowerCase(Locale.ROOT).contains("refund")) {
                CustomerAccount pharmacy = requirePharmacy();
                log.info("Account refund idempotent txn={}", request.transactionRef());
                return DebitResponse.refunded(
                        request.transactionRef(),
                        existing.get().getAmount(),
                        customer.getAvailableBalance(),
                        pharmacy.getAvailableBalance());
            }
        }

        AccountTransaction debit = findCustomerDebit(request.transactionRef());
        if (debit == null) {
            log.info("Account refund skipped, no customer debit txn={} order={}",
                    request.transactionRef(), request.orderNumber());
            return DebitResponse.nothingToRefund(
                    customer.getAvailableBalance() == null ? BigDecimal.ZERO : customer.getAvailableBalance());
        }
        BigDecimal amount = debit.getAmount();
        if (amount == null || amount.signum() <= 0) {
            return DebitResponse.nothingToRefund(
                    customer.getAvailableBalance() == null ? BigDecimal.ZERO : customer.getAvailableBalance());
        }

        BigDecimal next = (customer.getAvailableBalance() == null ? BigDecimal.ZERO : customer.getAvailableBalance()).add(amount);
        customer.setAvailableBalance(next);
        customer.setUpdatedAt(Instant.now());
        accounts.saveAndFlush(customer);
        post(customer, "CREDIT", amount, next, request,
                "Refund " + safe(request.orderNumber()) + " · order cancelled");

        CustomerAccount pharmacy = debitPharmacy(request, amount);
        hub.broadcast(toSnapshot(customer, true));
        hub.broadcast(toSnapshot(pharmacy, true));
        String gatewayRef = "RF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Account refunded email={} amount={} order={}",
                customer.getEmail(), amount, request.orderNumber());
        return DebitResponse.refunded(gatewayRef, amount, next, pharmacy.getAvailableBalance());
    }

    private AccountTransaction findCustomerDebit(String transactionRef) {
        if (transactionRef == null || transactionRef.isBlank()) {
            return null;
        }
        return transactions.findFirstByTransactionRefAndTypeOrderByCreatedAtAsc(transactionRef, "DEBIT")
                .filter(row -> row.getAccount() != null && !isPharmacy(row.getAccount()))
                .orElse(null);
    }

    private boolean isPharmacy(CustomerAccount account) {
        return account != null && pharmacyEmail.equals(normalize(account.getEmail()));
    }

    private CustomerAccount debitPharmacy(DebitRequest request, BigDecimal amount) {
        CustomerAccount pharmacy = requirePharmacy();
        if (request.transactionRef() != null && !request.transactionRef().isBlank()) {
            var existing = transactions.findByTransactionRefAndTypeAndAccount(
                    request.transactionRef(), "DEBIT", pharmacy);
            if (existing.isPresent()) {
                return pharmacy;
            }
        }
        BigDecimal current = pharmacy.getAvailableBalance() == null ? BigDecimal.ZERO : pharmacy.getAvailableBalance();
        BigDecimal remaining = current.subtract(amount);
        if (remaining.signum() < 0) {
            log.warn("Pharmacy refund debit clamped email={} required={}",
                    pharmacy.getEmail(), amount);
            remaining = BigDecimal.ZERO;
        }
        pharmacy.setAvailableBalance(remaining);
        pharmacy.setUpdatedAt(Instant.now());
        accounts.saveAndFlush(pharmacy);
        post(pharmacy, "DEBIT", amount, remaining, request,
                "Refund " + safe(request.orderNumber()) + " · to " + safe(request.email()));
        return pharmacy;
    }

    private CustomerAccount creditPharmacy(DebitRequest request, BigDecimal amount) {
        CustomerAccount pharmacy = requirePharmacy();
        if (request.transactionRef() != null && !request.transactionRef().isBlank()) {
            var existing = transactions.findByTransactionRefAndTypeAndAccount(
                    request.transactionRef(), "CREDIT", pharmacy);
            if (existing.isPresent()) {
                return pharmacy;
            }
        }
        BigDecimal current = pharmacy.getAvailableBalance() == null ? BigDecimal.ZERO : pharmacy.getAvailableBalance();
        BigDecimal next = current.add(amount);
        pharmacy.setAvailableBalance(next);
        pharmacy.setUpdatedAt(Instant.now());
        accounts.saveAndFlush(pharmacy);
        post(pharmacy, "CREDIT", amount, next, request,
                "Sale " + safe(request.orderNumber()) + " · from " + safe(request.email()));
        log.info("Pharmacy credited amount={} order={}", amount, request.orderNumber());
        return pharmacy;
    }

    private void post(
            CustomerAccount account,
            String type,
            BigDecimal amount,
            BigDecimal balanceAfter,
            DebitRequest request,
            String description) {
        AccountTransaction row = new AccountTransaction();
        row.setAccount(account);
        row.setType(type);
        row.setAmount(amount);
        row.setBalanceAfter(balanceAfter);
        row.setOrderNumber(request == null ? null : request.orderNumber());
        row.setTransactionRef(request == null ? null : request.transactionRef());
        row.setPaymentMode(request == null ? null : request.paymentMode());
        row.setDescription(description);
        transactions.save(row);
    }

    private CustomerAccount requireAccount(String email, Long userId, String customerName) {
        if (pharmacyEmail.equals(normalize(email))) {
            return requirePharmacy();
        }
        return accounts.lockByEmail(normalize(email)).orElseGet(() -> open(
                email, userId, customerName, CUSTOMER, openingBalance, "Opening balance"));
    }

    private CustomerAccount lockOrOpen(String email, Long userId, String customerName) {
        return accounts.lockByEmail(normalize(email)).orElseGet(() -> {
            try {
                return open(email, userId, customerName, CUSTOMER, openingBalance, "Opening balance");
            } catch (DataIntegrityViolationException ex) {
                return accounts.lockByEmail(normalize(email)).orElseThrow(() -> ex);
            }
        });
    }

    private CustomerAccount requirePharmacy() {
        return accounts.lockByEmail(pharmacyEmail).orElseGet(() -> {
            try {
                return open(pharmacyEmail, null, pharmacyName, MERCHANT, BigDecimal.ZERO, "Pharmacy account opened");
            } catch (DataIntegrityViolationException ex) {
                return accounts.lockByEmail(pharmacyEmail).orElseThrow(() -> ex);
            }
        });
    }

    private CustomerAccount open(
            String email,
            Long userId,
            String customerName,
            String type,
            BigDecimal opening,
            String description) {
        CustomerAccount account = new CustomerAccount();
        account.setEmail(normalize(email));
        account.setUserId(userId);
        account.setCustomerName(customerName);
        account.setAccountType(type);
        account.setAvailableBalance(opening);
        account.setUpdatedAt(Instant.now());
        accounts.saveAndFlush(account);

        if (opening != null && opening.signum() > 0) {
            AccountTransaction openingTxn = new AccountTransaction();
            openingTxn.setAccount(account);
            openingTxn.setType("CREDIT");
            openingTxn.setAmount(opening);
            openingTxn.setBalanceAfter(opening);
            openingTxn.setDescription(description);
            transactions.save(openingTxn);
        }
        log.info("Opened {} account email={}", type, account.getEmail());
        hub.broadcast(toSnapshot(account, true));
        return account;
    }

    private AccountSnapshot toSnapshot(CustomerAccount account, boolean includeTxns) {
        List<AccountSnapshot.AccountTxn> txns = List.of();
        if (includeTxns) {
            txns = transactions.findByAccountOrderByCreatedAtDesc(account).stream()
                    .map(row -> new AccountSnapshot.AccountTxn(
                            row.getId(),
                            row.getType(),
                            row.getAmount(),
                            row.getBalanceAfter(),
                            row.getOrderNumber(),
                            row.getTransactionRef(),
                            row.getPaymentMode(),
                            row.getDescription(),
                            row.getCreatedAt()))
                    .toList();
        }
        return new AccountSnapshot(
                account.getId(),
                account.getUserId(),
                account.getEmail(),
                account.getCustomerName(),
                account.getAccountType() == null ? CUSTOMER : account.getAccountType(),
                account.getAvailableBalance(),
                account.getUpdatedAt(),
                txns);
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
