package com.pharmacy.payment.kafka;

import com.pharmacy.payment.event.PaymentCommand;
import com.pharmacy.payment.event.PaymentResult;
import com.pharmacy.payment.service.PaymentProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class PaymentCommandListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentCommandListener.class);
    private final PaymentProcessingService payments;

    public PaymentCommandListener(PaymentProcessingService payments) {
        this.payments = payments;
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-commands}", groupId = "payment-service")
    @SendTo("${app.kafka.topics.payment-replies}")
    public PaymentResult onCommand(PaymentCommand command) {
        if (command == null) {
            return PaymentResult.fail(null, "Payment command was empty");
        }
        log.info("Payment command received action={} orderNumber={} txn={}",
                command.action(), command.orderNumber(), command.transactionRef());
        try {
            return payments.handle(command);
        } catch (Exception ex) {
            log.error("Payment command failed action={} orderNumber={}",
                    command.action(), command.orderNumber(), ex);
            try {
                return payments.failClosed(command, ex.getMessage() == null
                        ? "Payment could not be processed. Please try again."
                        : ex.getMessage());
            } catch (Exception nested) {
                log.error("Payment fail-closed also failed orderNumber={}", command.orderNumber(), nested);
                return PaymentResult.fail(command, "Payment could not be processed. Please try again.");
            }
        }
    }
}
