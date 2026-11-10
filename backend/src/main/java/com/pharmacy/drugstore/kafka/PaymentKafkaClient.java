package com.pharmacy.drugstore.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class PaymentKafkaClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentKafkaClient.class);
    private final ReplyingKafkaTemplate<String, Object, PaymentResult> template;
    private final String commandsTopic;

    public PaymentKafkaClient(
            ReplyingKafkaTemplate<String, Object, PaymentResult> paymentReplyKafkaTemplate,
            @Value("${app.kafka.topics.payment-commands}") String commandsTopic) {
        this.template = paymentReplyKafkaTemplate;
        this.commandsTopic = commandsTopic;
    }

    public PaymentResult refund(PaymentCommand command) {
        PaymentCommand refund = withAction(command, "REFUND");
        log.info("Payment refund published orderNumber={} topic={}", refund.orderNumber(), commandsTopic);
        return send(refund, "Refund service did not respond. Please try again.");
    }

    public PaymentResult charge(PaymentCommand command) {
        PaymentCommand charge = withAction(command, command.action() == null ? "CHARGE" : command.action());
        log.info("Payment command published orderNumber={} mode={} action={} topic={}",
                charge.orderNumber(), charge.paymentMode(), charge.action(), commandsTopic);
        return send(charge, "Payment service did not respond. Please try again.");
    }

    private PaymentResult send(PaymentCommand command, String timeoutMessage) {
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(commandsTopic, command.orderNumber(), command);
        try {
            RequestReplyFuture<String, Object, PaymentResult> future = template.sendAndReceive(record);
            PaymentResult result = future.get(20, TimeUnit.SECONDS).value();
            if (result == null) {
                return failed(command, "Payment service returned an empty result");
            }
            log.info("Payment result received orderNumber={} success={} message={}",
                    command.orderNumber(), result.success(), result.message());
            return result;
        } catch (Exception ex) {
            log.error("Payment kafka request failed orderNumber={}", command.orderNumber(), ex);
            return failed(command, timeoutMessage);
        }
    }

    private static PaymentCommand withAction(PaymentCommand command, String action) {
        if (command == null) {
            return null;
        }
        return new PaymentCommand(
                command.correlationId(),
                command.orderNumber(),
                command.transactionRef(),
                command.paymentMode(),
                command.amount(),
                command.userId(),
                command.customerName(),
                command.customerEmail(),
                command.upiId(),
                command.cardNumber(),
                command.cardHolder(),
                command.expiry(),
                command.cvv(),
                command.bankName(),
                command.walletProvider(),
                command.walletPhone(),
                action);
    }

    private static PaymentResult failed(PaymentCommand command, String message) {
        return new PaymentResult(
                command.correlationId(),
                command.orderNumber(),
                command.transactionRef(),
                false,
                null,
                message,
                null,
                null,
                null,
                null,
                null);
    }
}
