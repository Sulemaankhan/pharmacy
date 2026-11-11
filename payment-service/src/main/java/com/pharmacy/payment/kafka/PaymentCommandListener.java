package com.pharmacy.payment.kafka;

import com.pharmacy.payment.event.PaymentCommand;
import com.pharmacy.payment.event.PaymentResult;
import com.pharmacy.payment.gateway.DebitRequest;
import com.pharmacy.payment.gateway.DebitResponse;
import com.pharmacy.payment.gateway.PaymentGatewayClient;
import com.pharmacy.payment.payment.PaymentMode;
import com.pharmacy.payment.strategy.PaymentContext;
import com.pharmacy.payment.strategy.PaymentOutcome;
import com.pharmacy.payment.strategy.PaymentStrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class PaymentCommandListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);
    private final PaymentStrategyRegistry strategies;
    private final PaymentGatewayClient gateway;

    public PaymentCommandListener(PaymentStrategyRegistry strategies, PaymentGatewayClient gateway) {
        this.strategies = strategies;
        this.gateway = gateway;
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-commands}", groupId = "payment-service")
    @SendTo
    public PaymentResult onCommand(PaymentCommand command) {
        if (command == null) {
            return PaymentResult.fail(null, "Payment command was empty");
        }
        String action = command.action() == null || command.action().isBlank()
                ? "CHARGE" : command.action().trim().toUpperCase();
        if ("REFUND".equals(action)) {
            return refund(command);
        }
        return charge(command);
    }

    private PaymentResult charge(PaymentCommand command) {
        log.info("Payment charge received orderNumber={} mode={} amount={} email={}",
                command.orderNumber(), command.paymentMode(), command.amount(), command.customerEmail());
        try {
            PaymentMode mode = PaymentMode.valueOf(command.paymentMode());
            PaymentOutcome outcome = strategies.of(mode)
                    .process(new PaymentContext(command.amount(), command.orderNumber(), command));
            if (!outcome.success()) {
                log.info("Payment instrument declined orderNumber={} message={}",
                        command.orderNumber(), outcome.message());
                return PaymentResult.fail(command, outcome.message(), outcome.maskedInstrument(), null);
            }

            DebitResponse debit = gateway.debit(toGatewayRequest(command));
            if (debit == null || !debit.success()) {
                String message = debit == null ? "Payment gateway declined the charge" : debit.message();
                log.warn("Account debit rejected orderNumber={} message={}", command.orderNumber(), message);
                return PaymentResult.fail(command, message, outcome.maskedInstrument(),
                        debit == null ? null : debit.availableBalance());
            }

            String ref = debit.gatewayRef() != null ? debit.gatewayRef() : outcome.gatewayRef();
            log.info("Payment captured orderNumber={} debited={} remaining={} pharmacyCredited={} pharmacyBalance={}",
                    command.orderNumber(), debit.debitedAmount(), debit.availableBalance(),
                    debit.creditedAmount(), debit.pharmacyBalance());
            return new PaymentResult(
                    command.correlationId(),
                    command.orderNumber(),
                    command.transactionRef(),
                    true,
                    ref,
                    outcome.message() + " Debited ₹" + amount(debit.debitedAmount())
                            + ". Credited pharmacy ₹" + amount(debit.creditedAmount())
                            + ". Pharmacy balance ₹" + amount(debit.pharmacyBalance()) + ".",
                    outcome.maskedInstrument(),
                    debit.debitedAmount(),
                    debit.availableBalance(),
                    debit.creditedAmount(),
                    debit.pharmacyBalance());
        } catch (Exception ex) {
            log.warn("Payment processing failed orderNumber={} reason={}", command.orderNumber(), ex.getMessage());
            return PaymentResult.fail(command, ex.getMessage() == null ? "Payment could not be processed" : ex.getMessage());
        }
    }

    private PaymentResult refund(PaymentCommand command) {
        log.info("Payment refund received orderNumber={} amount={} email={}",
                command.orderNumber(), command.amount(), command.customerEmail());
        try {
            DebitResponse refund = gateway.refund(toGatewayRequest(command));
            if (refund == null || !refund.success()) {
                String message = refund == null ? "Payment gateway declined the refund" : refund.message();
                log.warn("Account refund rejected orderNumber={} message={}", command.orderNumber(), message);
                return PaymentResult.fail(command, message, null,
                        refund == null ? null : refund.availableBalance());
            }
            log.info("Payment refunded orderNumber={} credited={} remaining={} pharmacyBalance={}",
                    command.orderNumber(), refund.creditedAmount(), refund.availableBalance(), refund.pharmacyBalance());
            return new PaymentResult(
                    command.correlationId(),
                    command.orderNumber(),
                    command.transactionRef(),
                    true,
                    refund.gatewayRef(),
                    "Refunded ₹" + amount(refund.creditedAmount())
                            + " to customer. Pharmacy balance ₹" + amount(refund.pharmacyBalance()) + ".",
                    null,
                    BigDecimal.ZERO,
                    refund.availableBalance(),
                    refund.creditedAmount(),
                    refund.pharmacyBalance());
        } catch (Exception ex) {
            log.warn("Payment refund failed orderNumber={} reason={}", command.orderNumber(), ex.getMessage());
            return PaymentResult.fail(command, ex.getMessage() == null ? "Refund could not be processed" : ex.getMessage());
        }
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

    private static String amount(BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
    }
}
