package com.pharmacy.gateway.controller;

import com.pharmacy.gateway.dto.AccountSnapshot;
import com.pharmacy.gateway.dto.DebitRequest;
import com.pharmacy.gateway.dto.DebitResponse;
import com.pharmacy.gateway.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/accounts")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public AccountSnapshot get(
            @RequestParam String email,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String customerName) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        return accounts.snapshot(email, userId, customerName);
    }

    @GetMapping("/pharmacy")
    public AccountSnapshot pharmacy() {
        return accounts.pharmacySnapshot();
    }

    @GetMapping("/transactions/{transactionRef}")
    public DebitResponse lookup(@PathVariable String transactionRef) {
        return accounts.lookup(transactionRef);
    }

    @PostMapping("/debit")
    public DebitResponse debit(@RequestBody DebitRequest request) {
        return accounts.debit(request);
    }

    @PostMapping("/refund")
    public DebitResponse refund(@RequestBody DebitRequest request) {
        return accounts.refund(request);
    }
}
