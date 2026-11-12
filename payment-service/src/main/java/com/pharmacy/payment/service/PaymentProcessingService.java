package com.pharmacy.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pharmacy.payment.entity.PaymentIntent;
import com.pharmacy.payment.event.DomainEvent;
import com.pharmacy.payment.event.PaymentCommand;
import com.pharmacy.payment.event.PaymentResult;
import com.pharmacy.payment.gateway.DebitRequest;
import com.pharmacy.payment.gateway.DebitResponse;
import com.pharmacy.payment.gateway.PaymentGatewayClient;
import com.pharmacy.payment.payment.PaymentMode;
import com.pharmacy.payment.reliability.OutboxService;
import com.pharmacy.payment.repository.PaymentIntentRepository;
import com.pharmacy.payment.strategy.PaymentContext;
import com.pharmacy.payment.strategy.PaymentOutcome;
import com.pharmacy.payment.strategy.PaymentStrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentProcessingService {
    private static final Logger log = LoggerFactory.getLogger(PaymentProcessingService.class);

    private final PaymentStrategyRegistry strategies;
    private final PaymentGatewayClient gateway;
    private final PaymentIntentRepository intents;
    private final OutboxService outbox;
    private final ObjectMapper mapper;
    private final String paymentEvents;

    public PaymentProcessingService(
            PaymentStrategyRegistry strategies,
            PaymentGatewayClient gateway,
            PaymentIntentRepository intents,
            OutboxService outbox,
            ObjectMapper mapper,
            @Value("${app.kafka.topics.payment-events}") String paymentEvents) {
        this.strategies = strategies;
        this.gateway = gateway;
        this.intents = intents;
        this.outbox = outbox;
        this.mapper = mapper;
        this.paymentEvents = paymentEvents;
    }

    @Transactional
    public PaymentResult handle(PaymentCommand command) {
        if (command == null) {
            return PaymentResult.fail(null, "Payment command was empty");
        }
        String action = command.action() == null || command.action().isBlank()
                ? "CHARGE" : command.action().trim().toUpperCase();
        String key = key(command, action);
        PaymentIntent existing = intents.findByIdempotencyKey(key).orElse(null);
        if (existing != null && ("SUCCESS".equals(existing.getStatus()) || "FAILED".equals(existing.getStatus()))) {
            return republish(existing);
        }
        if (existing != null && ("PENDING".equals(existing.getStatus()) || "UNKNOWN".equals(existing.getStatus()))) {
            return reconcile(existing, command);
        }
        PaymentIntent intent = new PaymentIntent();
        intent.setIdempotencyKey(key);
        intent.setOrderNumber(command.orderNumber());
        intent.setTransactionRef(command.transactionRef());
        intent.setAction(action);
        intent.setStatus("PENDING");
        intent.setCommandJson(write(command));
        intent.setUpdatedAt(Instant.now());
        try {
            intents.saveAndFlush(intent);
        } catch (DataIntegrityViolationException ex) {
            PaymentIntent raced = intents.findByIdempotencyKey(key).orElseThrow();
            return handle(commandFrom(raced, command));
        }
        PaymentResult result = "REFUND".equals(action) ? refund(command) : charge(command);
        persist(intent, result, command);
        return result;
    }

    @Transactional
    public PaymentResult failClosed(PaymentCommand command, String message) {
        PaymentResult result = PaymentResult.fail(command, sanitize(message));
        if (command == null) {
            return result;
        }
        String action = command.action() == null || command.action().isBlank()
                ? "CHARGE" : command.action().trim().toUpperCase();
        PaymentIntent intent = intents.findByIdempotencyKey(key(command, action)).orElse(null);
        if (intent != null && ("SUCCESS".equals(intent.getStatus()) || "FAILED".equals(intent.getStatus()))) {
            return republish(intent);
        }
        if (intent == null) {
            intent = new PaymentIntent();
            intent.setIdempotencyKey(key(command, action));
            intent.setOrderNumber(command.orderNumber());
            intent.setTransactionRef(command.transactionRef());
            intent.setAction(action);
            intent.setCommandJson(write(command));
        }
        persist(intent, result, command);
        return result;
    }

    @Transactional
    public PaymentResult reconcile(PaymentIntent intent, PaymentCommand command) {
        PaymentCommand source = command != null ? command : read(intent.getCommandJson(), PaymentCommand.class);
        if (source == null) {
            return PaymentResult.fail(null, "Payment intent is missing its original command");
        }
        DebitResponse lookup = gateway.lookup(source.transactionRef());
        if (lookup != null && lookup.success()) {
            PaymentResult result = toSuccess(source, lookup, "Reconciled with payment gateway.");
            persist(intent, result, source);
            log.info("Payment reconciled orderNumber={} txn={}", source.orderNumber(), source.transactionRef());
            return result;
        }
        String message = lookup != null && lookup.message() != null && !lookup.message().isBlank()
                ? lookup.message()
                : PaymentGatewayClient.UNAVAILABLE;
        if (unavailable(lookup)) {
            PaymentResult result = PaymentResult.fail(source, message);
            persist(intent, result, source);
            log.warn("Payment gateway unavailable during reconcile orderNumber={}", source.orderNumber());
            return result;
        }
        PaymentResult result = PaymentResult.fail(source,
                "Payment was not completed. Please try again.");
        persist(intent, result, source);
        log.warn("Payment not found at gateway, failing orderNumber={}", source.orderNumber());
        return result;
    }

    private PaymentResult charge(PaymentCommand command) {
        log.info("Payment charge received orderNumber={} mode={} amount={}",
                command.orderNumber(), command.paymentMode(), command.amount());
        try {
            PaymentMode mode = PaymentMode.valueOf(command.paymentMode());
            PaymentOutcome outcome = strategies.of(mode)
                    .process(new PaymentContext(command.amount(), command.orderNumber(), command));
            if (!outcome.success()) {
                return PaymentResult.fail(command, outcome.message(), outcome.maskedInstrument(), null);
            }
            DebitResponse debit = gateway.debit(toGatewayRequest(command));
            if (debit == null || unavailable(debit)) {
                return PaymentResult.fail(command, debit == null
                        ? "Payment gateway returned an empty result"
                        : debit.message(), outcome.maskedInstrument(), null);
            }
            if (!debit.success()) {
                return PaymentResult.fail(command, debit.message(), outcome.maskedInstrument(), debit.availableBalance());
            }
            String ref = debit.gatewayRef() != null ? debit.gatewayRef() : outcome.gatewayRef();
            return new PaymentResult(
                    command.correlationId(),
                    command.orderNumber(),
                    command.transactionRef(),
                    true,
                    ref,
                    outcome.message() + " Debited ₹" + amount(debit.debitedAmount())
                            + ". Credited pharmacy ₹" + amount(debit.creditedAmount()) + ".",
                    outcome.maskedInstrument(),
                    debit.debitedAmount(),
                    debit.availableBalance(),
                    debit.creditedAmount(),
                    debit.pharmacyBalance());
        } catch (IllegalArgumentException ex) {
            return PaymentResult.fail(command, ex.getMessage() == null ? "Unsupported payment mode" : ex.getMessage());
        } catch (Exception ex) {
            log.warn("Payment processing failed orderNumber={} reason={}", command.orderNumber(), ex.getMessage());
            return PaymentResult.fail(command, sanitize(ex.getMessage()));
        }
    }

    private PaymentResult refund(PaymentCommand command) {
        log.info("Payment refund received orderNumber={} amount={}", command.orderNumber(), command.amount());
        try {
            DebitResponse refund = gateway.refund(toGatewayRequest(command));
            if (refund == null || unavailable(refund)) {
                return PaymentResult.fail(command, refund == null
                        ? "Payment gateway returned an empty refund result"
                        : refund.message());
            }
            if (!refund.success()) {
                return PaymentResult.fail(command, refund.message());
            }
            return new PaymentResult(
                    command.correlationId(),
                    command.orderNumber(),
                    command.transactionRef(),
                    true,
                    refund.gatewayRef(),
                    "Refunded ₹" + amount(refund.creditedAmount()) + " to customer.",
                    null,
                    BigDecimal.ZERO,
                    refund.availableBalance(),
                    refund.creditedAmount(),
                    refund.pharmacyBalance());
        } catch (Exception ex) {
            log.warn("Payment refund failed orderNumber={} reason={}", command.orderNumber(), ex.getMessage());
            return PaymentResult.fail(command, sanitize(ex.getMessage()));
        }
    }

    private void persist(PaymentIntent intent, PaymentResult result, PaymentCommand command) {
        boolean success = result != null && result.success();
        DomainEvent event = toEvent(command, result, success);
        intent.setStatus(success ? "SUCCESS" : "FAILED");
        intent.setEventId(event.getEventId());
        intent.setGatewayRef(result == null ? null : result.gatewayRef());
        intent.setMessage(result == null ? null : result.message());
        intent.setResultJson(write(result));
        intent.setUpdatedAt(Instant.now());
        intents.save(intent);
        outbox.enqueue(paymentEvents, command.orderNumber(), event);
    }

    private PaymentResult republish(PaymentIntent intent) {
        PaymentResult stored = read(intent.getResultJson(), PaymentResult.class);
        PaymentCommand command = read(intent.getCommandJson(), PaymentCommand.class);
        if (stored == null || command == null) {
            return stored;
        }
        DomainEvent event = toEvent(command, stored, stored.success());
        if (intent.getEventId() != null) {
            event.setEventId(intent.getEventId());
        }
        outbox.enqueue(paymentEvents, intent.getOrderNumber(), event);
        return stored;
    }

    private DomainEvent toEvent(PaymentCommand command, PaymentResult result, boolean success) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", success);
        payload.put("transactionRef", command.transactionRef());
        payload.put("message", result == null ? null : result.message());
        payload.put("gatewayRef", result == null ? null : result.gatewayRef());
        payload.put("maskedInstrument", result == null ? null : result.maskedInstrument());
        payload.put("debitedAmount", result == null ? null : result.debitedAmount());
        payload.put("availableBalance", result == null ? null : result.availableBalance());
        payload.put("creditedAmount", result == null ? null : result.creditedAmount());
        payload.put("pharmacyBalance", result == null ? null : result.pharmacyBalance());
        String type = "REFUND".equalsIgnoreCase(command.action())
                ? (success ? "PAYMENT_REFUNDED" : "PAYMENT_REFUND_FAILED")
                : (success ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED");
        return DomainEvent.of(type, command.orderNumber(), command.correlationId(), payload);
    }

    private static boolean unavailable(DebitResponse response) {
        String message = response == null ? null : response.message();
        return message != null && message.toLowerCase().contains("unavailable");
    }

    private static String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "Payment could not be processed. Please try again.";
        }
        String trimmed = message.length() > 300 ? message.substring(0, 300) : message;
        String lower = trimmed.toLowerCase();
        if (lower.contains("connection refused")
                || lower.contains("connect timed out")
                || lower.contains("connection timed out")
                || lower.contains("unknown host")
                || lower.contains("no route to host")
                || lower.contains("i/o error")
                || lower.contains("failed to connect")) {
            return PaymentGatewayClient.UNAVAILABLE;
        }
        return trimmed;
    }

    private static DebitRequest toGatewayRequest(PaymentCommand command) {
        return new DebitRequest(
                command.userId(),
                command.customerEmail(),
                command.customerName(),
                command.amount(),
                command.orderNumber(),
                command.transactionRef(),
                command.paymentMode());
    }

    private static PaymentResult toSuccess(PaymentCommand command, DebitResponse debit, String extra) {
        return new PaymentResult(
                command.correlationId(),
                command.orderNumber(),
                command.transactionRef(),
                true,
                debit.gatewayRef(),
                extra + " Debited ₹" + amount(debit.debitedAmount())
                        + ". Credited pharmacy ₹" + amount(debit.creditedAmount()) + ".",
                null,
                debit.debitedAmount(),
                debit.availableBalance(),
                debit.creditedAmount(),
                debit.pharmacyBalance());
    }

    private static String key(PaymentCommand command, String action) {
        return action + ":" + (command.transactionRef() == null ? command.orderNumber() : command.transactionRef());
    }

    private static PaymentCommand commandFrom(PaymentIntent intent, PaymentCommand fallback) {
        return fallback;
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize payment payload", ex);
        }
    }

    private <T> T read(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, type);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String amount(BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
    }
}
