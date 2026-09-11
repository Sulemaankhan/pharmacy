package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.dto.PaymentRequest;
import com.pharmacy.drugstore.dto.PaymentResponse;
import com.pharmacy.drugstore.entity.CartItem;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.OrderItem;
import com.pharmacy.drugstore.entity.PaymentTransaction;
import com.pharmacy.drugstore.entity.Product;
import com.pharmacy.drugstore.entity.Shipment;
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
import com.pharmacy.drugstore.logging.RequestMdc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
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
    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);
    private static final BigDecimal FREE_SHIPPING_AT = new BigDecimal("499");
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("49");

    private final CartItemRepository cartItems;
    private final CustomerOrderRepository orders;
    private final PaymentTransactionRepository transactions;
    private final ProductRepository products;
    private final PaymentStrategyRegistry strategies;
    private final LedgerPostingService ledger;
    private final NotificationFactory notifications;
    private final ShipmentService shipments;

    public CheckoutService(
            CartItemRepository cartItems,
            CustomerOrderRepository orders,
            PaymentTransactionRepository transactions,
            ProductRepository products,
            PaymentStrategyRegistry strategies,
            LedgerPostingService ledger,
            NotificationFactory notifications,
            ShipmentService shipments) {
        this.cartItems = cartItems;
        this.orders = orders;
        this.transactions = transactions;
        this.products = products;
        this.strategies = strategies;
        this.ledger = ledger;
        this.notifications = notifications;
        this.shipments = shipments;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.REPEATABLE_READ)
    public PaymentResponse checkout(User user, PaymentRequest request) {
        List<CartItem> cart = cartItems.findByUserId(user.getId());
        log.info("Checkout start {} cartItems={} mode={}",
                RequestMdc.describe(user), cart.size(), request == null ? null : request.paymentMode());
        if (cart.isEmpty()) {
            log.warn("Checkout aborted empty cart {}", RequestMdc.describe(user));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
        }
        shipments.requireDelivery(request, user);

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
        shipments.attach(order, user, request);
        log.info("Checkout order created {} orderNumber={} items={} subtotal={} shipping={} total={}",
                RequestMdc.describe(user), order.getOrderNumber(), order.getItems().size(), subtotal, shipping, total);

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
        log.info("Checkout payment {} orderNumber={} mode={} success={} message={}",
                RequestMdc.describe(user), order.getOrderNumber(), mode, outcome.success(), outcome.message());
        txn.setGatewayMessage(outcome.message());
        txn.setMaskedInstrument(outcome.maskedInstrument());
        txn.setCompletedAt(Instant.now());

        if (!outcome.success()) {
            txn.setStatus(PaymentStatus.FAILED);
            order.setStatus(PaymentStatus.FAILED);
            shipments.markPaymentResult(order.getShipment(), false);
            transactions.saveAndFlush(txn);
            orders.saveAndFlush(order);
            String emailStatus = sendOrderNotification(NotificationKind.PAYMENT_FAILURE, user, order, txn);
            log.warn("Checkout failed {} orderNumber={} txn={} email={}",
                    RequestMdc.describe(user), order.getOrderNumber(), txn.getTransactionRef(), emailStatus);
            return toResponse(false, order, txn,
                    outcome.message() + " " + emailPhrase(emailStatus, recipients(user, order)),
                    emailStatus);
        }

        if (outcome.gatewayRef() != null) {
            txn.setTransactionRef(outcome.gatewayRef());
        }
        txn.setStatus(PaymentStatus.SUCCESS);
        order.setStatus(PaymentStatus.SUCCESS);
        shipments.markPaymentResult(order.getShipment(), true);
        ledger.post(txn);

        for (CartItem item : cart) {
            Product product = locked.get(item.getProduct().getId());
            product.setStock(product.getStock() - item.getQuantity());
            products.save(product);
        }
        cartItems.deleteByUserId(user.getId());
        transactions.saveAndFlush(txn);
        orders.saveAndFlush(order);

        String emailStatus = sendOrderNotification(NotificationKind.PAYMENT_SUCCESS, user, order, txn);
        log.info("Checkout success {} orderNumber={} txn={} ledgerPosted={} email={}",
                RequestMdc.describe(user), order.getOrderNumber(), txn.getTransactionRef(), txn.isLedgerPosted(), emailStatus);
        return toResponse(true, order, txn,
                "Payment posted to ledger. " + emailPhrase(emailStatus, recipients(user, order)),
                emailStatus);
    }

    @Transactional
    public List<CustomerOrder> history(User user) {
        List<CustomerOrder> list = orders.findByUserIdOrderByCreatedAtDesc(user.getId());
        list.forEach(order -> shipments.refresh(order.getShipment()));
        log.info("Order history {} count={}", RequestMdc.describe(user), list.size());
        return list;
    }

    @Transactional
    public CustomerOrder get(User user, String orderNumber) {
        log.info("Order get {} orderNumber={}", RequestMdc.describe(user), orderNumber);
        CustomerOrder order = orders.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> {
                    log.warn("Order not found {} orderNumber={}", RequestMdc.describe(user), orderNumber);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
                });
        shipments.refresh(order.getShipment());
        return order;
    }

    public Map<String, String> emailDetails(User user, String orderNumber) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your account does not have an email ID");
        }
        CustomerOrder order = get(user, orderNumber);
        log.info("Order email {} orderNumber={} to={}",
                RequestMdc.describe(user), order.getOrderNumber(), user.getEmail());
        notifications.create(NotificationKind.ORDER_DETAILS).send(user, order, order.getPayment());
        String recipient = user.getEmail();
        if (order.getShipment() != null && order.getShipment().getEmail() != null
                && !order.getShipment().getEmail().isBlank()
                && !order.getShipment().getEmail().equalsIgnoreCase(user.getEmail())) {
            recipient = user.getEmail() + " and " + order.getShipment().getEmail();
        }
        return Map.of(
                "status", "SENT",
                "recipient", recipient,
                "orderNumber", order.getOrderNumber(),
                "message", "Order details were emailed to " + recipient);
    }

    public PaymentTransaction getTransaction(String ref) {
        return transactions.findByTransactionRef(ref)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private Map<Long, Product> lockStock(List<CartItem> cart) {
        List<Long> ids = cart.stream().map(item -> item.getProduct().getId()).distinct().toList();
        return products.lockAllById(ids).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private String sendOrderNotification(NotificationKind kind, User user, CustomerOrder order, PaymentTransaction txn) {
        try {
            log.info("Notification send {} kind={} orderNumber={} to={}",
                    RequestMdc.describe(user), kind, order.getOrderNumber(), user.getEmail());
            notifications.create(kind).send(user, order, txn);
            return "SENT";
        } catch (Exception ex) {
            log.warn("Notification failed {} kind={} orderNumber={} reason={}",
                    RequestMdc.describe(user), kind, order.getOrderNumber(), ex.getMessage());
            return "FAILED";
        }
    }

    private static String recipients(User user, CustomerOrder order) {
        String account = user.getEmail();
        if (order.getShipment() != null && order.getShipment().getEmail() != null
                && !order.getShipment().getEmail().isBlank()
                && !order.getShipment().getEmail().equalsIgnoreCase(account)) {
            return account + " and " + order.getShipment().getEmail().trim();
        }
        return account;
    }

    private static String emailPhrase(String emailStatus, String email) {
        if ("SENT".equals(emailStatus)) {
            return "A notification was emailed to " + email + ".";
        }
        return "We could not email a notification to " + email + ". Use Email order details to retry.";
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
        Shipment shipment = order.getShipment();
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
                items,
                shipment == null ? null : shipment.getTrackingNumber(),
                shipment == null ? null : shipment.getRecipientName(),
                shipment == null ? null : shipment.getFullAddress(),
                shipment == null ? null : shipment.getContactNumber(),
                shipment == null ? null : shipment.getEmail());
    }
}
