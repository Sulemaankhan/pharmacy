package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.PaymentRequest;
import com.pharmacy.drugstore.dto.PaymentResponse;
import com.pharmacy.drugstore.entity.CartItem;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.OrderItem;
import com.pharmacy.drugstore.entity.PaymentTransaction;
import com.pharmacy.drugstore.entity.Product;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.notification.NotificationFactory;
import com.pharmacy.drugstore.notification.NotificationKind;
import com.pharmacy.drugstore.payment.PaymentMode;
import com.pharmacy.drugstore.payment.PaymentStatus;
import com.pharmacy.drugstore.payment.strategy.PaymentContext;
import com.pharmacy.drugstore.payment.strategy.PaymentOutcome;
import com.pharmacy.drugstore.payment.strategy.PaymentStrategyRegistry;
import com.pharmacy.drugstore.repository.CartItemRepository;
import com.pharmacy.drugstore.repository.CustomerOrderRepository;
import com.pharmacy.drugstore.repository.PaymentTransactionRepository;
import com.pharmacy.drugstore.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {
    private static final BigDecimal FREE_SHIPPING_AT = new BigDecimal("499");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("49");

    private final CartItemRepository cartItems;
    private final CustomerOrderRepository orders;
    private final PaymentTransactionRepository transactions;
    private final ProductRepository products;
    private final PaymentStrategyRegistry strategies;
    private final LedgerPostingService ledger;
    private final NotificationFactory notifications;

    public CheckoutService(
            CartItemRepository cartItems,
            CustomerOrderRepository orders,
            PaymentTransactionRepository transactions,
            ProductRepository products,
            PaymentStrategyRegistry strategies,
            LedgerPostingService ledger,
            NotificationFactory notifications) {
        this.cartItems = cartItems;
        this.orders = orders;
        this.transactions = transactions;
        this.products = products;
        this.strategies = strategies;
        this.ledger = ledger;
        this.notifications = notifications;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public PaymentResponse checkout(User user, PaymentRequest request) {
        List<CartItem> cart = cartItems.findByUser(user);
        if (cart.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }

        PaymentMode mode = parseMode(request == null ? null : request.paymentMode());
        Map<Long, Product> locked = lockStock(cart);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart) {
            Product product = locked.get(item.getProduct().getId());
            if (product.getStock() == null || product.getStock() < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient stock for " + product.getName());
            }
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_AT) >= 0 ? BigDecimal.ZERO : SHIPPING_FEE;
        BigDecimal total = subtotal.add(shipping);

        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(nextOrderNumber());
        order.setUser(user);
        order.setStatus(PaymentStatus.PENDING);
        order.setSubtotal(subtotal);
        order.setShipping(shipping);
        order.setTotal(total);
        for (CartItem cartItem : cart) {
            Product product = locked.get(cartItem.getProduct().getId());
            OrderItem line = new OrderItem();
            line.setOrder(order);
            line.setProductId(product.getId());
            line.setProductName(product.getName());
            line.setImageUrl(product.getImageUrl());
            line.setUnitPrice(product.getPrice());
            line.setQuantity(cartItem.getQuantity());
            order.getItems().add(line);
        }
        orders.saveAndFlush(order);

        PaymentTransaction txn = new PaymentTransaction();
        txn.setTransactionRef("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        txn.setPaymentMode(mode);
        txn.setStatus(PaymentStatus.PENDING);
        txn.setAmount(total);
        txn.setOrder(order);
        transactions.saveAndFlush(txn);
        order.setPayment(txn);

        PaymentOutcome outcome = strategies.of(mode)
                .process(new PaymentContext(user, total, order.getOrderNumber(), request));
        txn.setGatewayMessage(outcome.message());
        txn.setMaskedInstrument(outcome.maskedInstrument());
        txn.setCompletedAt(Instant.now());

        if (!outcome.success()) {
            txn.setStatus(PaymentStatus.FAILED);
            order.setStatus(PaymentStatus.FAILED);
            transactions.saveAndFlush(txn);
            orders.saveAndFlush(order);
            queueNotification(NotificationKind.PAYMENT_FAILURE, user, order, txn);
            return toResponse(false, order, txn, outcome.message(), "QUEUED");
        }

        if (outcome.gatewayRef() != null) {
            txn.setTransactionRef(outcome.gatewayRef());
        }
        txn.setStatus(PaymentStatus.SUCCESS);
        order.setStatus(PaymentStatus.SUCCESS);
        ledger.post(txn);

        for (CartItem item : cart) {
            Product product = locked.get(item.getProduct().getId());
            product.setStock(product.getStock() - item.getQuantity());
            products.save(product);
        }
        cartItems.deleteByUser(user);
        transactions.saveAndFlush(txn);
        orders.saveAndFlush(order);

        queueNotification(NotificationKind.PAYMENT_SUCCESS, user, order, txn);
        return toResponse(true, order, txn, "Payment posted to ledger. Confirmation will be emailed to " + user.getEmail(),
                "QUEUED");
    }

    public List<CustomerOrder> history(User user) {
        return orders.findByUserOrderByCreatedAtDesc(user);
    }

    public CustomerOrder get(User user, String orderNumber) {
        return orders.findByOrderNumberAndUser(orderNumber, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    public PaymentTransaction getTransaction(String ref) {
        return transactions.findByTransactionRef(ref)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private Map<Long, Product> lockStock(List<CartItem> cart) {
        List<Long> ids = cart.stream().map(item -> item.getProduct().getId()).distinct().toList();
        return products.lockAllById(ids).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private void queueNotification(NotificationKind kind, User user, CustomerOrder order, PaymentTransaction txn) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notifications.create(kind).send(user, order, txn);
            }
        });
    }

    private PaymentMode parseMode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a payment mode");
        }
        try {
            return PaymentMode.valueOf(raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment mode");
        }
    }

    private String nextOrderNumber() {
        return "MD-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private PaymentResponse toResponse(boolean success, CustomerOrder order, PaymentTransaction txn,
                                       String message, String emailStatus) {
        List<PaymentResponse.Item> items = order.getItems().stream()
                .map(i -> new PaymentResponse.Item(i.getProductName(), i.getQuantity(), i.getUnitPrice()))
                .toList();
        return new PaymentResponse(
                success,
                order.getId(),
                txn.getId(),
                order.getOrderNumber(),
                txn.getTransactionRef(),
                txn.getPaymentMode(),
                txn.getStatus(),
                txn.getAmount(),
                txn.isLedgerPosted(),
                message,
                emailStatus,
                txn.getCompletedAt(),
                items);
    }
}
