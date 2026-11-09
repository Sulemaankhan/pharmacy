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
import com.pharmacy.drugstore.kafka.PaymentKafkaClient;
import com.pharmacy.drugstore.kafka.PaymentResult;
import com.pharmacy.drugstore.kafka.ShipmentPublisher;
import com.pharmacy.drugstore.payment.PaymentMode;
import com.pharmacy.drugstore.payment.PaymentStatus;
import com.pharmacy.drugstore.repository.CartItemRepository;
import com.pharmacy.drugstore.repository.CustomerOrderRepository;
import com.pharmacy.drugstore.repository.PaymentTransactionRepository;
import com.pharmacy.drugstore.repository.ProductRepository;
import com.pharmacy.drugstore.logging.RequestMdc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final PaymentKafkaClient payments;
    private final NotificationPublisher notifications;
    private final ShipmentPublisher shipmentEvents;
    private final TransactionTemplate tx;

    public CheckoutService(
            CartItemRepository cartItems,
            CustomerOrderRepository orders,
            PaymentTransactionRepository transactions,
            ProductRepository products,
            LedgerPostingService ledger,
            ShipmentService shipments,
            PaymentKafkaClient payments,
            NotificationPublisher notifications,
            ShipmentPublisher shipmentEvents,
            PlatformTransactionManager transactionManager) {
        this.cartItems = cartItems;
        this.orders = orders;
        this.transactions = transactions;
        this.products = products;
        this.ledger = ledger;
        this.shipments = shipments;
        this.payments = payments;
        this.notifications = notifications;
        this.shipmentEvents = shipmentEvents;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    public PaymentResponse checkout(User user, PaymentRequest request) {
        log.info("Checkout start {} mode={}",
                RequestMdc.describe(user), request == null ? null : request.paymentMode());
        shipments.requireDelivery(request, user);
        PaymentMode mode = parseMode(request == null ? null : request.paymentMode());

        CustomerOrder pending = tx.execute(status -> {
            List<CartItem> cart = cartItems.findByUserId(user.getId());
            log.info("Checkout cart {} cartItems={}", RequestMdc.describe(user), cart.size());
            if (cart.isEmpty()) {
                log.warn("Checkout aborted empty cart {}", RequestMdc.describe(user));
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your cart is empty");
            }
            return createPendingOrder(user, request, cart, mode);
        });
        shipmentEvents.created(user, pending);
        PaymentCommand command = toPaymentCommand(user, pending, request, mode);
        PaymentResult result = payments.charge(command);
        CompletedCheckout completed = tx.execute(status -> completePayment(user.getId(), pending.getOrderNumber(), result));
        shipmentEvents.paymentResult(user, completed.order(), result.success());
        String emailStatus = publishOrderNotification(
                result.success() ? "PAYMENT_SUCCESS" : "PAYMENT_FAILURE",
                user, completed.order(), completed.txn());
        log.info("Checkout finished {} orderNumber={} success={} email={}",
                RequestMdc.describe(user), completed.order().getOrderNumber(), result.success(), emailStatus);
        return toResponse(result.success(), completed.order(), completed.txn(),
                completed.message() + " " + emailPhrase(emailStatus, recipients(user, completed.order())),
                emailStatus);
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

        for (CartItem item : cart) {
            Product product = locked.get(item.getProduct().getId());
            product.setStock(product.getStock() - item.getQuantity());
            products.save(product);
        }

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

    private CompletedCheckout completePayment(Long userId, String orderNumber, PaymentResult result) {
        CustomerOrder order = orders.findByOrderNumberAndUserId(orderNumber, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        PaymentTransaction txn = order.getPayment();
        if (txn == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is missing a payment transaction");
        }
        txn.setGatewayMessage(result.message());
        txn.setMaskedInstrument(result.maskedInstrument());
        txn.setCompletedAt(Instant.now());

        if (!result.success()) {
            txn.setStatus(PaymentStatus.FAILED);
            order.setStatus(PaymentStatus.FAILED);
            shipments.markPaymentResult(order.getShipment(), false);
            restoreStock(order);
            transactions.saveAndFlush(txn);
            orders.saveAndFlush(order);
            log.warn("Checkout failed orderNumber={} txn={} message={}",
                    order.getOrderNumber(), txn.getTransactionRef(), result.message());
            return new CompletedCheckout(order, txn, result.message());
        }

        if (result.gatewayRef() != null) {
            txn.setTransactionRef(result.gatewayRef());
        }
        txn.setStatus(PaymentStatus.SUCCESS);
        order.setStatus(PaymentStatus.SUCCESS);
        shipments.markPaymentResult(order.getShipment(), true);
        ledger.post(txn);
        cartItems.deleteByUserId(userId);
        transactions.saveAndFlush(txn);
        orders.saveAndFlush(order);
        log.info("Checkout success orderNumber={} txn={} ledgerPosted={}",
                order.getOrderNumber(), txn.getTransactionRef(), txn.isLedgerPosted());
        return new CompletedCheckout(order, txn, "Payment posted to ledger.");
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

    private void restoreStock(CustomerOrder order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        List<Long> ids = order.getItems().stream().map(OrderItem::getProductId).distinct().toList();
        Map<Long, Product> locked = products.lockAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (OrderItem item : order.getItems()) {
            Product product = locked.get(item.getProductId());
            if (product == null || product.getStock() == null) {
                continue;
            }
            product.setStock(product.getStock() + item.getQuantity());
            products.save(product);
        }
    }

    private Map<Long, Product> lockStock(List<CartItem> cart) {
        List<Long> ids = cart.stream().map(item -> item.getProduct().getId()).distinct().toList();
        return products.lockAllById(ids).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private static PaymentCommand toPaymentCommand(User user, CustomerOrder order, PaymentRequest request, PaymentMode mode) {
        PaymentTransaction txn = order.getPayment();
        return new PaymentCommand(
                UUID.randomUUID().toString(),
                order.getOrderNumber(),
                txn.getTransactionRef(),
                mode.name(),
                txn.getAmount(),
                user.getName(),
                user.getEmail(),
                request == null ? null : request.upiId(),
                request == null ? null : request.cardNumber(),
                request == null ? null : request.cardHolder(),
                request == null ? null : request.expiry(),
                request == null ? null : request.cvv(),
                request == null ? null : request.bankName(),
                request == null ? null : request.walletProvider(),
                request == null ? null : request.walletPhone());
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

    private record CompletedCheckout(CustomerOrder order, PaymentTransaction txn, String message) {}
}
