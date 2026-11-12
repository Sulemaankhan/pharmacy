package com.pharmacy.drugstore.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.drugstore.dto.GatewayDebitRequest;
import com.pharmacy.drugstore.dto.GatewayDebitResponse;
import com.pharmacy.drugstore.dto.PaymentRequest;
import com.pharmacy.drugstore.dto.PaymentResponse;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.kafka.DomainEvent;
import com.pharmacy.drugstore.kafka.PaymentCommand;
import com.pharmacy.drugstore.kafka.PaymentGatewayClient;
import com.pharmacy.drugstore.kafka.PaymentResult;
import com.pharmacy.drugstore.kafka.ShipmentCommand;
import com.pharmacy.drugstore.kafka.StockCommand;
import com.pharmacy.drugstore.logging.RequestMdc;
import com.pharmacy.drugstore.payment.PaymentMode;
import com.pharmacy.drugstore.reliability.OutboxService;
import com.pharmacy.drugstore.repository.CustomerOrderRepository;
import com.pharmacy.drugstore.repository.UserRepository;
import com.pharmacy.drugstore.service.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutSagaOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(CheckoutSagaOrchestrator.class);
    private static final Duration CHECKOUT_WAIT = Duration.ofSeconds(30);
    private static final Duration INVENTORY_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration PAYMENT_TIMEOUT = Duration.ofSeconds(12);
    private static final Duration SHIPMENT_TIMEOUT = Duration.ofMinutes(5);
    private static final String INVENTORY_TIMEOUT_REASON =
            "Stock reservation did not complete in time. The order was rolled back.";
    private static final String PAYMENT_TIMEOUT_REASON =
            "Payment was not confirmed in time. The payment gateway may be unavailable. The order was rolled back.";
    private static final String SHIPMENT_TIMEOUT_REASON =
            "Shipment service did not respond within 5 minutes. Inventory, payment, and the order were rolled back.";

    private final CheckoutService checkout;
    private final CheckoutSagaRepository sagas;
    private final CustomerOrderRepository orders;
    private final UserRepository users;
    private final OutboxService outbox;
    private final PaymentGatewayClient gateway;
    private final SagaWaitRegistry waits;
    private final ObjectMapper mapper;
    private final TransactionTemplate tx;
    private final String orderEvents;
    private final String stockCommands;
    private final String paymentCommands;
    private final String fulfilmentEvents;
    private final String shipmentCommands;

    public CheckoutSagaOrchestrator(
            CheckoutService checkout,
            CheckoutSagaRepository sagas,
            CustomerOrderRepository orders,
            UserRepository users,
            OutboxService outbox,
            PaymentGatewayClient gateway,
            SagaWaitRegistry waits,
            ObjectMapper mapper,
            PlatformTransactionManager transactionManager,
            @Value("${app.kafka.topics.order-events}") String orderEvents,
            @Value("${app.kafka.topics.stock-commands}") String stockCommands,
            @Value("${app.kafka.topics.payment-commands}") String paymentCommands,
            @Value("${app.kafka.topics.fulfilment-events}") String fulfilmentEvents,
            @Value("${app.kafka.topics.shipment-commands}") String shipmentCommands) {
        this.checkout = checkout;
        this.sagas = sagas;
        this.orders = orders;
        this.users = users;
        this.outbox = outbox;
        this.gateway = gateway;
        this.waits = waits;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(transactionManager);
        this.orderEvents = orderEvents;
        this.stockCommands = stockCommands;
        this.paymentCommands = paymentCommands;
        this.fulfilmentEvents = fulfilmentEvents;
        this.shipmentCommands = shipmentCommands;
    }

    public PaymentResponse execute(User user, PaymentRequest request, PaymentMode mode) {
        log.info("Checkout saga start {} mode={}", RequestMdc.describe(user), mode);
        CustomerOrder order = tx.execute(status -> {
            CustomerOrder created = checkout.reserveStock(user, request, mode);
            start(user, created, request, mode);
            return created;
        });
        waits.register(order.getOrderNumber());
        PaymentResponse response = waits.await(order.getOrderNumber(), CHECKOUT_WAIT);
        if (response != null) {
            return response;
        }
        return tx.execute(status -> {
            CheckoutSaga saga = requireSaga(order.getOrderNumber());
            if (saga.isCompensated() || "COMPENSATED".equals(saga.getStatus())) {
                return failedResponse(saga, null);
            }
            if (waitingOnInventory(saga) && !saga.isStockReserved()) {
                return compensate(saga, null, saga.isPaymentCaptured(), INVENTORY_TIMEOUT_REASON);
            }
            if (waitingOnPayment(saga) && !saga.isPaymentCaptured()) {
                return compensate(saga, null, saga.isPaymentCaptured(), PAYMENT_TIMEOUT_REASON);
            }
            CustomerOrder current = reload(order.getOrderNumber());
            log.warn("Checkout saga still running after wait orderNumber={} step={}",
                    order.getOrderNumber(), saga.getStep());
            return checkout.toPaymentResponse(false, current, current.getPayment(),
                    "Your order could not be confirmed in time. We will email you when it succeeds or fails.",
                    "PENDING", null);
        });
    }

    @Transactional
    public PaymentResponse onInventoryReserved(DomainEvent event) {
        CheckoutSaga saga = requireSaga(event.getOrderNumber());
        if (saga.isCompensated()) {
            CustomerOrder order = reload(saga.getOrderNumber());
            outbox.enqueue(stockCommands, saga.getOrderNumber(), "RELEASE",
                    commandEventId(saga, "RELEASE"),
                    checkout.releaseCommand(order));
            log.info("Saga released late inventory reserve orderNumber={}", saga.getOrderNumber());
            return null;
        }
        if (saga.isPaymentCaptured()) {
            return null;
        }
        saga.setStockReserved(true);
        moveTo(saga, "INVENTORY_RESERVED");
        saga.setLastEventId(event.getEventId());
        save(saga);
        PaymentCommand command = read(saga.getPaymentCommandJson(), PaymentCommand.class);
        outbox.enqueue(paymentCommands, saga.getOrderNumber(), "CHARGE", saga.getTransactionRef(), command);
        log.info("Saga inventory reserved, payment commanded orderNumber={}", saga.getOrderNumber());
        return null;
    }

    @Transactional
    public PaymentResponse onInventoryFailed(DomainEvent event) {
        CheckoutSaga saga = requireSaga(event.getOrderNumber());
        String reason = event.str("message");
        return compensate(saga, toPaymentResult(event), false,
                reason == null ? "Inventory could not reserve stock" : reason);
    }

    @Transactional
    public PaymentResponse onPaymentSuccess(DomainEvent event) {
        CheckoutSaga saga = requireSaga(event.getOrderNumber());
        PaymentResult payment = toPaymentResult(event);
        if (saga.isCompensated()) {
            reverseGatewayCharge(saga);
            log.info("Saga refunded late payment orderNumber={}", saga.getOrderNumber());
            return null;
        }
        User user = users.findById(saga.getUserId()).orElseThrow();
        if (!saga.isPaymentCaptured()) {
            checkout.recordCapture(user.getId(), saga.getOrderNumber(), payment);
            saga.setPaymentCaptured(true);
        }
        moveTo(saga, "PAYMENT_SUCCESS");
        saga.setLastEventId(event.getEventId());
        save(saga);

        var completed = checkout.completeSuccess(user.getId(), saga.getOrderNumber(), payment);
        String emailStatus = checkout.notifyOrder("PAYMENT_SUCCESS", user, completed.order(), completed.txn());
        DomainEvent confirmed = DomainEvent.of("ORDER_CONFIRMED", saga.getOrderNumber(), saga.getTransactionRef(),
                payload("status", "CONFIRMED"));
        outbox.enqueue(orderEvents, saga.getOrderNumber(), confirmed);
        moveTo(saga, "ORDER_CONFIRMED");
        saga.setStatus("RUNNING");
        save(saga);
        log.info("Saga payment success, order confirmed orderNumber={}", saga.getOrderNumber());
        return checkout.toPaymentResponse(true, completed.order(), completed.txn(),
                "Your order is confirmed. " + notificationSentence(emailStatus, true),
                emailStatus, payment);
    }

    @Transactional
    public PaymentResponse onPaymentFailed(DomainEvent event) {
        CheckoutSaga saga = requireSaga(event.getOrderNumber());
        String reason = event.str("message");
        return compensate(saga, toPaymentResult(event), saga.isPaymentCaptured(),
                reason == null ? "Payment failed" : reason);
    }

    @Transactional
    public void onOrderConfirmed(DomainEvent event) {
        CheckoutSaga saga = requireSaga(event.getOrderNumber());
        if (saga.isCompensated() || saga.isPharmacyReady()) {
            return;
        }
        checkout.markFulfillment(saga.getOrderNumber(), "PACKED");
        saga.setPharmacyReady(true);
        moveTo(saga, "PHARMACY_READY");
        saga.setLastEventId(event.getEventId());
        save(saga);
        DomainEvent packed = DomainEvent.of("PACKAGE_READY", saga.getOrderNumber(), saga.getTransactionRef(),
                payload("status", "PACKED"));
        outbox.enqueue(fulfilmentEvents, saga.getOrderNumber(), packed);
        ShipmentCommand command = read(saga.getShipmentCommandJson(), ShipmentCommand.class);
        if (command != null) {
            outbox.enqueue(shipmentCommands, saga.getOrderNumber(), "PACKAGE_READY",
                    commandEventId(saga, "PACKAGE_READY"),
                    withType(command, "PACKAGE_READY"));
        }
        log.info("Saga pharmacy packed orderNumber={}", saga.getOrderNumber());
    }

    @Transactional
    public void onShipmentCreated(DomainEvent event) {
        CheckoutSaga saga = requireSaga(event.getOrderNumber());
        if (saga.isCompensated()) {
            ShipmentCommand cancel = read(saga.getShipmentCommandJson(), ShipmentCommand.class);
            if (cancel != null) {
                outbox.enqueue(shipmentCommands, saga.getOrderNumber(), "CANCEL",
                        commandEventId(saga, "CANCEL"),
                        withType(cancel, "CANCEL"));
            }
            log.info("Saga cancelled late shipment orderNumber={}", saga.getOrderNumber());
            return;
        }
        saga.setShipmentReserved(true);
        moveTo(saga, "SHIPMENT_CREATED");
        saga.setLastEventId(event.getEventId());
        save(saga);
        checkout.markFulfillment(saga.getOrderNumber(), "SHIPMENT_CREATED");
    }

    @Transactional
    public void onShipmentFailed(DomainEvent event) {
        CheckoutSaga saga = requireSaga(event.getOrderNumber());
        String reason = event.str("message");
        compensate(saga, null, true, reason == null ? "Shipment could not be created" : reason);
    }

    @Transactional
    public void onShipmentPickedUp(DomainEvent event) {
        CheckoutSaga saga = requireSaga(event.getOrderNumber());
        if (saga.isCompensated()) {
            return;
        }
        moveTo(saga, "SHIPMENT_PICKED_UP");
        saga.setLastEventId(event.getEventId());
        save(saga);
        checkout.markFulfillment(saga.getOrderNumber(), "SHIPPED");
        User user = users.findById(saga.getUserId()).orElse(null);
        CustomerOrder order = reload(saga.getOrderNumber());
        if (user != null) {
            checkout.notifyOrder("SHIPMENT_PICKED_UP", user, order, order.getPayment());
        }
    }

    @Transactional
    public void onShipmentDelivered(DomainEvent event) {
        CheckoutSaga saga = requireSaga(event.getOrderNumber());
        if (saga.isCompensated() || saga.isDelivered()) {
            return;
        }
        var completed = checkout.markDelivered(saga.getOrderNumber());
        saga.setDelivered(true);
        saga.setStatus("COMPLETED");
        moveTo(saga, "DELIVERED");
        saga.setLastEventId(event.getEventId());
        save(saga);
        DomainEvent delivered = DomainEvent.of("ORDER_DELIVERED", saga.getOrderNumber(), saga.getTransactionRef(),
                payload("status", "DELIVERED"));
        outbox.enqueue(orderEvents, saga.getOrderNumber(), delivered);
        User user = users.findById(saga.getUserId()).orElse(null);
        if (user != null) {
            checkout.notifyOrder("ORDER_DELIVERED", user, completed.order(), completed.txn());
        }
        log.info("Saga order delivered orderNumber={}", saga.getOrderNumber());
    }

    @Scheduled(fixedDelay = 1000)
    public void enforceDeadlines() {
        Instant now = Instant.now();
        for (CheckoutSaga listed : sagas.findByStatusIn(List.of("RUNNING"))) {
            try {
                tx.execute(status -> {
                    CheckoutSaga saga = sagas.lockByOrderNumber(listed.getOrderNumber()).orElse(null);
                    if (saga == null || !"RUNNING".equals(saga.getStatus()) || saga.isCompensated()) {
                        return null;
                    }
                    if (saga.getStepStartedAt() == null) {
                        saga.setStepStartedAt(now);
                        save(saga);
                        return null;
                    }
                    if (waitingOnInventory(saga) && !saga.isStockReserved()
                            && timedOut(saga, INVENTORY_TIMEOUT, now)) {
                        log.warn("Inventory deadline exceeded orderNumber={} step={}",
                                saga.getOrderNumber(), saga.getStep());
                        compensate(saga, null, saga.isPaymentCaptured(), INVENTORY_TIMEOUT_REASON);
                    } else if (waitingOnPayment(saga) && !saga.isPaymentCaptured()
                            && timedOut(saga, PAYMENT_TIMEOUT, now)) {
                        log.warn("Payment deadline exceeded orderNumber={} step={}",
                                saga.getOrderNumber(), saga.getStep());
                        compensate(saga, null, saga.isPaymentCaptured(), PAYMENT_TIMEOUT_REASON);
                    } else if (waitingOnShipment(saga) && !saga.isShipmentReserved()
                            && timedOut(saga, SHIPMENT_TIMEOUT, now)) {
                        log.warn("Shipment deadline exceeded orderNumber={}", saga.getOrderNumber());
                        compensate(saga, null, saga.isPaymentCaptured(), SHIPMENT_TIMEOUT_REASON);
                    }
                    return null;
                });
            } catch (Exception ex) {
                log.error("Checkout deadline check failed orderNumber={}", listed.getOrderNumber(), ex);
            }
        }
    }

    @Scheduled(fixedDelay = 20000)
    public void recoverStuck() {
        Instant cutoff = Instant.now().minus(45, ChronoUnit.SECONDS);
        for (CheckoutSaga listed : sagas.findByStatusInAndUpdatedAtBefore(List.of("RUNNING", "COMPENSATING"), cutoff)) {
            try {
                tx.execute(status -> {
                    CheckoutSaga saga = sagas.findByOrderNumber(listed.getOrderNumber()).orElse(null);
                    if (saga == null || saga.isCompensated() || "COMPENSATED".equals(saga.getStatus())
                            || "COMPLETED".equals(saga.getStatus())) {
                        return null;
                    }
                    if ("RUNNING".equals(saga.getStatus()) && (waitingOnInventory(saga) || waitingOnShipment(saga))) {
                        return null;
                    }
                    log.warn("Checkout saga recovering orderNumber={} step={}", saga.getOrderNumber(), saga.getStep());
                    if ("COMPENSATING".equals(saga.getStatus())) {
                        compensate(saga, null, saga.isPaymentCaptured(),
                                saga.getFailureReason() == null ? "Checkout was rolled back" : saga.getFailureReason());
                    } else {
                        republish(saga);
                    }
                    return null;
                });
            } catch (Exception ex) {
                log.error("Checkout saga recovery failed orderNumber={}", listed.getOrderNumber(), ex);
            }
        }
    }

    private void republish(CheckoutSaga saga) {
        String step = saga.getStep() == null ? "" : saga.getStep();
        CustomerOrder order = reload(saga.getOrderNumber());
        switch (step) {
            case "ORDER_CREATED", "STOCK" -> outbox.enqueue(stockCommands, saga.getOrderNumber(),
                    "RESERVE", commandEventId(saga, "RESERVE"), checkout.stockCommand(order));
            case "INVENTORY_RESERVED", "PAYMENT" -> {
                PaymentCommand command = read(saga.getPaymentCommandJson(), PaymentCommand.class);
                if (command != null) {
                    outbox.enqueue(paymentCommands, saga.getOrderNumber(), "CHARGE", saga.getTransactionRef(), command);
                }
            }
            case "PAYMENT_SUCCESS", "ORDER_CONFIRMED" -> outbox.enqueue(orderEvents, saga.getOrderNumber(),
                    DomainEvent.of("ORDER_CONFIRMED", saga.getOrderNumber(), saga.getTransactionRef(),
                            payload("status", "CONFIRMED")));
            case "PHARMACY_READY" -> {
                outbox.enqueue(fulfilmentEvents, saga.getOrderNumber(),
                        DomainEvent.of("PACKAGE_READY", saga.getOrderNumber(), saga.getTransactionRef(),
                                payload("status", "PACKED")));
                ShipmentCommand command = read(saga.getShipmentCommandJson(), ShipmentCommand.class);
                if (command != null) {
                    outbox.enqueue(shipmentCommands, saga.getOrderNumber(), "PACKAGE_READY",
                            commandEventId(saga, "PACKAGE_READY"), withType(command, "PACKAGE_READY"));
                }
            }
            default -> log.info("Saga recovery waiting on downstream step={} orderNumber={}",
                    step, saga.getOrderNumber());
        }
        saga.setUpdatedAt(Instant.now());
        sagas.save(saga);
    }

    private PaymentResponse compensate(
            CheckoutSaga saga,
            PaymentResult payment,
            boolean refundMoney,
            String reason) {
        String orderNumber = saga.getOrderNumber();
        saga = sagas.lockByOrderNumber(orderNumber).orElseThrow(() ->
                new IllegalStateException("Saga not found for " + orderNumber));
        if (saga.isCompensated() || "COMPENSATED".equals(saga.getStatus())) {
            PaymentResponse response = failedResponse(saga, payment);
            completeAfterCommit(orderNumber, response);
            return response;
        }
        User user = users.findById(saga.getUserId()).orElseThrow();
        CustomerOrder order = reload(orderNumber);
        saga.setStatus("COMPENSATING");
        saga.setFailureReason(trim(reason));
        save(saga);

        boolean refunded = reverseGatewayCharge(saga) || (refundMoney && saga.isPaymentCaptured());
        PaymentResult refund = payment;

        outbox.enqueue(stockCommands, saga.getOrderNumber(), "RELEASE",
                commandEventId(saga, "RELEASE"), checkout.releaseCommand(order));
        ShipmentCommand cancel = read(saga.getShipmentCommandJson(), ShipmentCommand.class);
        if (cancel != null) {
            outbox.enqueue(shipmentCommands, saga.getOrderNumber(), "CANCEL",
                    commandEventId(saga, "CANCEL"), withType(cancel, "CANCEL"));
        }
        var failed = checkout.failLocal(user.getId(), order.getOrderNumber(), reason, refunded);
        String emailStatus = checkout.notifyOrder("PAYMENT_FAILURE", user, failed.order(), failed.txn());
        saga.setCompensated(true);
        saga.setStockReserved(false);
        saga.setPaymentCaptured(false);
        saga.setShipmentReserved(false);
        saga.setStatus("COMPENSATED");
        moveTo(saga, "COMPENSATED");
        save(saga);
        DomainEvent cancelled = DomainEvent.of("ORDER_CANCELLED", saga.getOrderNumber(), saga.getTransactionRef(),
                payload("reason", trim(reason)));
        outbox.enqueue(orderEvents, saga.getOrderNumber(), cancelled);
        String message = "Your order was cancelled. " + notificationSentence(emailStatus, false)
                + (refunded ? " The charged amount was refunded to your account." : "")
                + " " + trim(reason);
        log.warn("Checkout saga compensated orderNumber={} reason={}", saga.getOrderNumber(), reason);
        PaymentResponse response = checkout.toPaymentResponse(false, failed.order(), failed.txn(),
                message, emailStatus, refund != null ? refund : payment);
        completeAfterCommit(saga.getOrderNumber(), response);
        return response;
    }

    private CheckoutSaga start(User user, CustomerOrder order, PaymentRequest request, PaymentMode mode) {
        CheckoutSaga saga = sagas.findByOrderNumber(order.getOrderNumber()).orElseGet(CheckoutSaga::new);
        saga.setOrderNumber(order.getOrderNumber());
        saga.setUserId(user.getId());
        saga.setCustomerEmail(user.getEmail());
        saga.setCustomerName(user.getName());
        saga.setTransactionRef(order.getPayment() == null ? null : order.getPayment().getTransactionRef());
        saga.setPaymentMode(mode == null ? null : mode.name());
        saga.setAmount(order.getTotal());
        saga.setStatus("RUNNING");
        saga.setStep("ORDER_CREATED");
        saga.setStepStartedAt(Instant.now());
        saga.setStockReserved(false);
        saga.setPaymentCaptured(false);
        saga.setPharmacyReady(false);
        saga.setShipmentReserved(false);
        saga.setDelivered(false);
        saga.setCompensated(false);
        saga.setPaymentCommandJson(write(checkout.paymentCommand(user, order, request, mode)));
        saga.setShipmentCommandJson(write(checkout.shipmentCommand("PACKAGE_READY", user, order, true)));
        save(saga);

        DomainEvent created = DomainEvent.of("ORDER_CREATED", order.getOrderNumber(), saga.getTransactionRef(),
                payload("amount", order.getTotal(), "userId", user.getId()));
        outbox.enqueue(orderEvents, order.getOrderNumber(), created);
        StockCommand stock = checkout.stockCommand(order);
        outbox.enqueue(stockCommands, order.getOrderNumber(), "RESERVE", commandEventId(saga, "RESERVE"), stock);
        log.info("Saga ORDER_CREATED outbox stock reserve orderNumber={}", order.getOrderNumber());
        return saga;
    }

    private CheckoutSaga requireSaga(String orderNumber) {
        return sagas.findByOrderNumber(orderNumber).orElseThrow(() ->
                new IllegalStateException("Saga not found for " + orderNumber));
    }

    private CheckoutSaga save(CheckoutSaga saga) {
        saga.setUpdatedAt(Instant.now());
        return sagas.saveAndFlush(saga);
    }

    private void moveTo(CheckoutSaga saga, String step) {
        if (step != null && !step.equals(saga.getStep())) {
            saga.setStep(step);
            saga.setStepStartedAt(Instant.now());
        } else if (saga.getStepStartedAt() == null) {
            saga.setStepStartedAt(Instant.now());
        }
    }

    private static boolean waitingOnInventory(CheckoutSaga saga) {
        String step = saga.getStep() == null ? "" : saga.getStep();
        return "ORDER_CREATED".equals(step) || "STOCK".equals(step);
    }

    private static boolean waitingOnPayment(CheckoutSaga saga) {
        String step = saga.getStep() == null ? "" : saga.getStep();
        return "INVENTORY_RESERVED".equals(step) || "PAYMENT".equals(step);
    }

    private static boolean waitingOnShipment(CheckoutSaga saga) {
        return "PHARMACY_READY".equals(saga.getStep());
    }

    private static boolean timedOut(CheckoutSaga saga, Duration timeout, Instant now) {
        Instant started = saga.getStepStartedAt();
        return started != null && !started.plus(timeout).isAfter(now);
    }

    private PaymentResponse failedResponse(CheckoutSaga saga, PaymentResult payment) {
        CustomerOrder order = reload(saga.getOrderNumber());
        String message = "Your order was cancelled. " + notificationSentence("QUEUED", false);
        if (saga.getFailureReason() != null && !saga.getFailureReason().isBlank()) {
            message = message + " " + saga.getFailureReason();
        }
        return checkout.toPaymentResponse(false, order, order.getPayment(), message, "FAILED", payment);
    }

    private void completeAfterCommit(String orderNumber, PaymentResponse response) {
        if (response == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    waits.complete(orderNumber, response);
                }
            });
            return;
        }
        waits.complete(orderNumber, response);
    }

    private static String notificationSentence(String emailStatus, boolean success) {
        boolean sent = "QUEUED".equals(emailStatus) || "SENT".equals(emailStatus);
        if (success) {
            return sent
                    ? "A success notification was emailed to you."
                    : "We could not send the success notification email.";
        }
        return sent
                ? "A failure notification was emailed to you."
                : "We could not send the failure notification email.";
    }

    private CustomerOrder reload(String orderNumber) {
        return orders.findByOrderNumber(orderNumber).orElseThrow();
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not store saga command", ex);
        }
    }

    private <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not read saga command", ex);
        }
    }

    private static PaymentCommand withAction(PaymentCommand command, String action) {
        return new PaymentCommand(
                command.correlationId(), command.orderNumber(), command.transactionRef(), command.paymentMode(),
                command.amount(), command.userId(), command.customerName(), command.customerEmail(),
                command.upiId(), command.cardNumber(), command.cardHolder(), command.expiry(), command.cvv(),
                command.bankName(), command.walletProvider(), command.walletPhone(), action);
    }

    private static ShipmentCommand withType(ShipmentCommand command, String type) {
        return new ShipmentCommand(
                type, command.userId(), command.customerEmail(), command.orderId(), command.orderNumber(),
                command.trackingNumber(), Boolean.TRUE.equals(command.paid()) || "PACKAGE_READY".equals(type),
                command.recipientName(), command.address(), command.city(), command.state(), command.pincode(),
                command.contactNumber(), command.email(), command.carrier(), command.estimatedDelivery(),
                command.createdAt(), command.correlationId());
    }

    private static PaymentResult toPaymentResult(DomainEvent event) {
        if (event == null) {
            return PaymentResult.fail("Payment event was empty");
        }
        boolean success = event.bool("success") || "PAYMENT_SUCCESS".equals(event.getEventType())
                || "INVENTORY_RESERVED".equals(event.getEventType());
        return new PaymentResult(
                event.getCorrelationId(),
                event.getOrderNumber(),
                event.str("transactionRef"),
                success,
                event.str("gatewayRef"),
                event.str("message"),
                event.str("maskedInstrument"),
                decimal(event.getPayload().get("debitedAmount")),
                decimal(event.getPayload().get("availableBalance")),
                decimal(event.getPayload().get("creditedAmount")),
                decimal(event.getPayload().get("pharmacyBalance")));
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal amount) {
            return amount;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Map<String, Object> payload(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return map;
    }

    private boolean reverseGatewayCharge(CheckoutSaga saga) {
        PaymentCommand command = read(saga.getPaymentCommandJson(), PaymentCommand.class);
        if (command == null) {
            return false;
        }
        outbox.enqueue(paymentCommands, saga.getOrderNumber(), "REFUND",
                commandEventId(saga, "REFUND"), withAction(command, "REFUND"));
        GatewayDebitResponse result;
        try {
            result = gateway.refund(new GatewayDebitRequest(
                    command.userId(),
                    command.customerEmail(),
                    command.customerName(),
                    command.amount(),
                    command.orderNumber(),
                    command.transactionRef(),
                    command.paymentMode()));
        } catch (Exception ex) {
            log.warn("Saga gateway refund failed orderNumber={} reason={}",
                    saga.getOrderNumber(), ex.getMessage());
            return false;
        }
        if (result != null && result.moneyReturned()) {
            checkout.markRefunded(saga.getUserId(), saga.getOrderNumber(), new PaymentResult(
                    command.correlationId(),
                    command.orderNumber(),
                    command.transactionRef(),
                    true,
                    result.gatewayRef(),
                    result.message(),
                    null,
                    BigDecimal.ZERO,
                    result.availableBalance(),
                    result.creditedAmount(),
                    result.pharmacyBalance()));
            log.info("Saga reversed gateway debit orderNumber={} credited={}",
                    saga.getOrderNumber(), result.creditedAmount());
            return true;
        }
        log.info("Saga gateway refund had nothing to reverse orderNumber={} message={}",
                saga.getOrderNumber(), result == null ? null : result.message());
        return false;
    }

    private static String commandEventId(CheckoutSaga saga, String action) {
        String ref = saga.getTransactionRef();
        if (ref == null || ref.isBlank()) {
            ref = saga.getOrderNumber();
        }
        return ref + ":" + action;
    }

    private static String trim(String value) {
        if (value == null || value.isBlank()) {
            return "Checkout was rolled back";
        }
        return value.length() > 480 ? value.substring(0, 480) : value;
    }
}
