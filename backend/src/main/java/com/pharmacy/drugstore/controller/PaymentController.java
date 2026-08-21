package com.pharmacy.drugstore.controller;

import com.pharmacy.drugstore.dto.PaymentRequest;
import com.pharmacy.drugstore.dto.PaymentResponse;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.payment.PaymentMode;
import com.pharmacy.drugstore.service.AuthService;
import com.pharmacy.drugstore.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final CheckoutService checkoutService;
    private final AuthService authService;

    public PaymentController(CheckoutService checkoutService, AuthService authService) {
        this.checkoutService = checkoutService;
        this.authService = authService;
    }

    @GetMapping("/payments/modes")
    public List<Map<String, String>> modes() {
        return Arrays.stream(PaymentMode.values())
                .map(mode -> Map.of(
                        "code", mode.name(),
                        "label", label(mode),
                        "hint", hint(mode)))
                .toList();
    }

    @PostMapping("/payments/checkout")
    public PaymentResponse checkout(Authentication auth, @RequestBody PaymentRequest request) {
        return checkoutService.checkout(user(auth), request);
    }

    @GetMapping("/orders")
    public List<CustomerOrder> orders(Authentication auth) {
        return checkoutService.history(user(auth));
    }

    @GetMapping("/orders/{orderNumber}")
    public CustomerOrder order(Authentication auth, @PathVariable String orderNumber) {
        return checkoutService.get(user(auth), orderNumber);
    }

    @GetMapping("/payments/transactions/{ref}")
    public com.pharmacy.drugstore.entity.PaymentTransaction transaction(@PathVariable String ref) {
        return checkoutService.getTransaction(ref);
    }

    private User user(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        return authService.requireUser(auth.getName());
    }

    private String label(PaymentMode mode) {
        return switch (mode) {
            case UPI -> "UPI";
            case CARD -> "Debit / Credit card";
            case NET_BANKING -> "Net banking";
            case WALLET -> "Wallet";
            case COD -> "Cash on delivery";
        };
    }

    private String hint(PaymentMode mode) {
        return switch (mode) {
            case UPI -> "Google Pay, PhonePe, Paytm UPI";
            case CARD -> "Visa, Mastercard, RuPay";
            case NET_BANKING -> "SBI, HDFC, ICICI and more";
            case WALLET -> "Paytm, PhonePe, Amazon Pay";
            case COD -> "Pay cash when delivered";
        };
    }
}
