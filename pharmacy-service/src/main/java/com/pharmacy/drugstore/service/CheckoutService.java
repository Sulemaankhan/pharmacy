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
import com.pharmacy.drugstore.kafka.NotificationCommand;
import com.pharmacy.drugstore.kafka.NotificationCommands;
import com.pharmacy.drugstore.kafka.NotificationPublisher;
import com.pharmacy.drugstore.kafka.PaymentCommand;
import com.pharmacy.drugstore.kafka.PaymentResult;
import com.pharmacy.drugstore.payment.PaymentMode;
import com.pharmacy.drugstore.saga.CheckoutSagaOrchestrator;
import com.pharmacy.drugstore.payment.PaymentStatus;
import com.pharmacy.drugstore.repository.CartItemRepository;
import com.pharmacy.drugstore.repository.CustomerOrderRepository;
import com.pharmacy.drugstore.repository.PaymentTransactionRepository;
import com.pharmacy.drugstore.repository.ProductRepository;
import com.pharmacy.drugstore.logging.RequestMdc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
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
    private final LedgerPostingService ledger;
    private final ShipmentService shipments;
    private final NotificationPublisher notifications;
    private final CheckoutSagaOrchestrator saga;
    private final TransactionTemplate tx;

    public CheckoutService(
            CartItemRepository cartItems,
            CustomerOrderRepository orders,
            PaymentTransactionRepository transactions,
            ProductRepository products,
            LedgerPostingService ledger,
            ShipmentService shipments,
            NotificationPublisher notifications,
            @Lazy CheckoutSagaOrchestrator saga,
            PlatformTransactionManager transactionManager) {
        this.cartItems = cartItems;
        this.orders = orders;
        this.transactions = transactions;
        this.products = products;
        this.ledger = ledger;
        this.shipments = shipments;
        this.notifications = notifications;
        this.saga = saga;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    public PaymentResponse checkout(User user, PaymentRequest request) {
        shipments.requireDelivery(request, user);
        PaymentMode mode = parseMode(request == null ? null : request.paymentMode());
        return saga.execute(user, request, mode);
    }

    public CustomerOrder reserveStock(User user, PaymentRequest request, PaymentMode mode) {
        return tx.execute(status -> {
            List<CartItem> cart = cartItems.findByUserId(user.getId());
            log.info("Checkout cart {} cartItems={}", RequestMdc.describe(user), cart.size());
            if (cart.isEmpty()) {
                log.warn("Checkout aborted empty cart {}", RequestMdc.describe(user));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
            }
            return createPendingOrder(user, request, cart, mode);
        });
    }

    public PaymentCommand paymentCommand(User user, CustomerOrder order, PaymentRequest request, PaymentMode mode) {
        return toPaymentCommand(user, order, request, mode);
    }

    public void recordCapture(Long userId, String orderNumber, PaymentResult result) {
        tx.execute(status -> {
            CustomerOrder order = orders.findByOrderNumberAndUserId(orderNumber, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
            PaymentTransaction txn = requireTxn(order);
            applyGateway(txn, result);
            transactions.saveAndFlush(txn);
            orders.saveAndFlush(order);
            return order;
        });
    }

    public CompletedCheckout completeSuccess(Long userId, String orderNumber, PaymentResult result) {
        return tx.execute(status -> {
            CustomerOrder order = orders.findByOrderNumberAndUserId(orderNumber, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
            PaymentTransaction txn = requireTxn(order);
            applyGateway(txn, result);
            txn.setStatus(PaymentStatus.SUCCESS);
            order.setStatus(PaymentStatus.SUCCESS);
            shipments.markPaymentResult(order.getShipment(), true);
            if (!txn.isLedgerPosted()) {
                ledger.post(txn);
            }
            cartItems.deleteByUserId(userId);
            transactions.saveAndFlush(txn);
            orders.saveAndFlush(order);
            log.info("Checkout success orderNumber={} txn={} ledgerPosted={}",
                    order.getOrderNumber(), txn.getTransactionRef(), txn.isLedgerPosted());
            return new CompletedCheckout(order, txn, "Payment posted to ledger.");
        });
    }

    public CompletedCheckout failLocal(Long userId, String orderNumber, String reason, boolean refunded) {
        return tx.execute(status -> {
            CustomerOrder order = orders.findByOrderNumberAndUserId(orderNumber, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
            PaymentTransaction txn = requireTxn(order);
            if (txn.isLedgerPosted()) {
                ledger.reverse(txn);
            }
            txn.setStatus(PaymentStatus.FAILED);
            txn.setGatewayMessage(reason);
            txn.setCompletedAt(Instant.now());
            if (refunded) {
                txn.setRefunded(true);
            }
            order.setStatus(PaymentStatus.FAILED);
            shipments.markPaymentResult(order.getShipment(), false);
            transactions.saveAndFlush(txn);
            orders.saveAndFlush(order);
            log.warn("Checkout rolled back orderNumber={} refunded={} reason={}",
                    order.getOrderNumber(), refunded, reason);
            return new CompletedCheckout(order, txn, reason);
        });
    }

    public void markRefunded(Long userId, String orderNumber, PaymentResult refund) {
        tx.execute(status -> {
            CustomerOrder order = orders.findByOrderNumberAndUserId(orderNumber, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
            PaymentTransaction txn = requireTxn(order);
            txn.setRefunded(true);
            txn.setRefundedAmount(refund == null ? txn.getAmount() : refund.creditedAmount());
            if (refund != null) {
                txn.setGatewayMessage(refund.message());
            }
            transactions.saveAndFlush(txn);
            return order;
        });
    }

    public String notifyOrder(String kind, User user, CustomerOrder order, PaymentTransaction txn) {
        return publishOrderNotification(kind, user, order, txn);
    }

    public String emailPhrase(String emailStatus, User user, CustomerOrder order) {
        return emailPhrase(emailStatus, recipients(user, order));
    }

    public PaymentResponse toPaymentResponse(
            boolean success,
            CustomerOrder order,
            PaymentTransaction txn,
            String message,
            String emailStatus,
            PaymentResult result) {
        return toResponse(success, order, txn, message, emailStatus, result);
    }

    public List<CustomerOrder> history(User user) {
        return tx.execute(status -> {
            List<CustomerOrder> list = orders.findByUserIdOrderByCreatedAtDesc(user.getId());
            log.info("Order history {} count={}", RequestMdc.describe(user), list.size());
            return list;
        });
    }

    public CustomerOrder get(User user, String orderNumber) {
        return tx.execute(status -> loadOrder(user, orderNumber));
    }

    public Map<String, String> emailDetails(User user, String orderNumber) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your account does not have an email ID");
        }
        CustomerOrder order = get(user, orderNumber);
        log.info("Order email queued {} orderNumber={} to={}",
                RequestMdc.describe(user), order.getOrderNumber(), user.getEmail());
        notifications.publish(NotificationCommands.forOrder("ORDER_DETAILS", user, order, order.getPayment()));
        String recipient = String.join(" and ", NotificationCommands.recipients(user, order));
        return Map.of(
                "status", "QUEUED",
                "recipient", recipient,
                "orderNumber", order.getOrderNumber(),
                "message", "Order details were queued for email to " + recipient);
    }

    public PaymentTransaction getTransaction(String ref) {
        return transactions.findByTransactionRef(ref)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private CustomerOrder createPendingOrder(User user, PaymentRequest request, List<CartItem> cart, PaymentMode mode) {
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

        PaymentTransaction txn = new PaymentTransaction();
        txn.setTransactionRef("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        txn.setPaymentMode(mode);
        txn.setStatus(PaymentStatus.PENDING);
        txn.setAmount(total);
        txn.setOrder(order);
        transactions.saveAndFlush(txn);
        order.setPayment(txn);
        log.info("Checkout order created {} orderNumber={} items={} total={}",
                RequestMdc.describe(user), order.getOrderNumber(), order.getItems().size(), total);
        return order;
    }

    private String publishOrderNotification(String kind, User user, CustomerOrder order, PaymentTransaction txn) {
        try {
            NotificationCommand command = NotificationCommands.forOrder(kind, user, order, txn);
            log.info("Notification queue {} kind={} orderNumber={} to={}",
                    RequestMdc.describe(user), kind, order.getOrderNumber(), user.getEmail());
            notifications.publish(command);
            return "QUEUED";
        } catch (Exception ex) {
            log.warn("Notification queue failed {} kind={} orderNumber={} reason={}",
                    RequestMdc.describe(user), kind, order.getOrderNumber(), ex.getMessage());
            return "FAILED";
        }
    }

    private CustomerOrder loadOrder(User user, String orderNumber) {
        log.info("Order get {} orderNumber={}", RequestMdc.describe(user), orderNumber);
        CustomerOrder order = orders.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> {
                    log.warn("Order not found {} orderNumber={}", RequestMdc.describe(user), orderNumber);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
                });
        return order;
    }

    private Map<Long, Product> lockStock(List<CartItem> cart) {
        List<Long> ids = cart.stream().map(item -> item.getProduct().getId()).distinct().toList();
        return products.lockAllById(ids).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private PaymentTransaction requireTxn(CustomerOrder order) {
        PaymentTransaction txn = order.getPayment();
        if (txn == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is missing a payment transaction");
        }
        return txn;
    }

    private static void applyGateway(PaymentTransaction txn, PaymentResult result) {
        if (result == null) {
            return;
        }
        txn.setGatewayMessage(result.message());
        txn.setMaskedInstrument(result.maskedInstrument());
        txn.setDebitedAmount(result.debitedAmount());
        txn.setCreditedAmount(result.creditedAmount());
        txn.setCompletedAt(Instant.now());
    }

    private static PaymentCommand toPaymentCommand(User user, CustomerOrder order, PaymentRequest request, PaymentMode mode) {
        PaymentTransaction txn = order.getPayment();
        PaymentMode resolved = mode != null ? mode : txn.getPaymentMode();
        return new PaymentCommand(
                UUID.randomUUID().toString(),
                order.getOrderNumber(),
                txn.getTransactionRef(),
                resolved == null ? null : resolved.name(),
                txn.getAmount(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                request == null ? null : request.upiId(),
                request == null ? null : request.cardNumber(),
                request == null ? null : request.cardHolder(),
                request == null ? null : request.expiry(),
                request == null ? null : request.cvv(),
                request == null ? null : request.bankName(),
                request == null ? null : request.walletProvider(),
                request == null ? null : request.walletPhone(),
                "CHARGE");
    }

    private static String recipients(User user, CustomerOrder order) {
        return String.join(" and ", NotificationCommands.recipients(user, order));
    }

    private static String emailPhrase(String emailStatus, String email) {
        if ("QUEUED".equals(emailStatus) || "SENT".equals(emailStatus)) {
            return "A notification was queued for email to " + email + ".";
        }
        return "We could not queue a notification for " + email + ". Use Email order details to retry.";
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
                                       String message, String emailStatus, PaymentResult result) {
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
                shipment == null ? null : shipment.getEmail(),
                result == null ? null : result.debitedAmount(),
                null,
                result == null ? null : result.creditedAmount(),
                null);
    }

    public record CompletedCheckout(CustomerOrder order, PaymentTransaction txn, String message) {}
}
