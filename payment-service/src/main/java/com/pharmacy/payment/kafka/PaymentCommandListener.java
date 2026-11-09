package com.pharmacy.payment.kafka;

import com.pharmacy.payment.event.PaymentCommand;
import com.pharmacy.payment.event.PaymentResult;
import com.pharmacy.payment.payment.PaymentMode;
import com.pharmacy.payment.strategy.PaymentContext;
import com.pharmacy.payment.strategy.PaymentOutcome;
import com.pharmacy.payment.strategy.PaymentStrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class PaymentCommandListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);
    private final PaymentStrategyRegistry strategies;

    public PaymentCommandListener(PaymentStrategyRegistry strategies) {
        this.strategies = strategies;
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-commands}", groupId = "payment-service")
    @SendTo
    public PaymentResult onCommand(PaymentCommand command) {
        if (command == null) {
            return new PaymentResult(null, null, null, false, null, "Payment command was empty", null);
        }
        log.info("Payment command received orderNumber={} mode={} amount={}",
                command.orderNumber(), command.paymentMode(), command.amount());
        try {
            PaymentMode mode = PaymentMode.valueOf(command.paymentMode());
            PaymentOutcome outcome = strategies.of(mode)
                    .process(new PaymentContext(command.amount(), command.orderNumber(), command));
            log.info("Payment processed orderNumber={} success={} message={}",
                    command.orderNumber(), outcome.success(), outcome.message());
            return new PaymentResult(
                    command.correlationId(),
                    command.orderNumber(),
                    command.transactionRef(),
                    outcome.success(),
                    outcome.gatewayRef(),
                    outcome.message(),
                    outcome.maskedInstrument());
        } catch (Exception ex) {
            log.warn("Payment processing failed orderNumber={} reason={}", command.orderNumber(), ex.getMessage());
            return new PaymentResult(
                    command.correlationId(),
                    command.orderNumber(),
                    command.transactionRef(),
                    false,
                    null,
                    ex.getMessage() == null ? "Payment could not be processed" : ex.getMessage(),
                    null);
        }
    }
}
