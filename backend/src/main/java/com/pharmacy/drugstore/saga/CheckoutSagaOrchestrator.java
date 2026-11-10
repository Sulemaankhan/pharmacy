package com.pharmacy.drugstore.saga;

import com.pharmacy.drugstore.dto.PaymentRequest;
import com.pharmacy.drugstore.dto.PaymentResponse;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.kafka.PaymentCommand;
import com.pharmacy.drugstore.kafka.PaymentKafkaClient;
import com.pharmacy.drugstore.kafka.PaymentResult;
import com.pharmacy.drugstore.kafka.ShipmentKafkaClient;
import com.pharmacy.drugstore.kafka.ShipmentResult;
import com.pharmacy.drugstore.logging.RequestMdc;
import com.pharmacy.drugstore.payment.PaymentMode;
import com.pharmacy.drugstore.repository.CustomerOrderRepository;
import com.pharmacy.drugstore.repository.UserRepository;
import com.pharmacy.drugstore.service.CheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class CheckoutSagaOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(CheckoutSagaOrchestrator.class);

    private final CheckoutService checkout;
    private final PaymentKafkaClient payments;
    private final ShipmentKafkaClient shipments;
    private final CheckoutSagaRepository sagas;
    private final CustomerOrderRepository orders;
    private final UserRepository users;

    public CheckoutSagaOrchestrator(
            CheckoutService checkout,
            PaymentKafkaClient payments,
            ShipmentKafkaClient shipments,
            CheckoutSagaRepository sagas,
            CustomerOrderRepository orders,
            UserRepository users) {
        this.checkout = checkout;
        this.payments = payments;
        this.shipments = shipments;
        this.sagas = sagas;
        this.orders = orders;
        this.users = users;
    }

    public PaymentResponse execute(User user, PaymentRequest request, PaymentMode mode) {
        log.info("Checkout saga start {} mode={}", RequestMdc.describe(user), mode);
        CustomerOrder order = checkout.reserveStock(user, request, mode);
        CheckoutSaga saga = start(user, order);
        PaymentCommand command = checkout.paymentCommand(user, order, request, mode);
        PaymentResult payment = PaymentResult.fail("Payment was not attempted");
        try {
            saga.setStep("PAYMENT");
            save(saga);
            payment = payments.charge(command);
            if (payment == null || !payment.success()) {
                return compensate(user, order, saga, payment, false,
                        payment == null ? "Payment failed" : payment.message());
            }
            saga.setPaymentCaptured(true);
            saga.setStep("SHIPMENT");
            save(saga);
            checkout.recordCapture(user.getId(), order.getOrderNumber(), payment);

            ShipmentResult shipment = shipments.reserve(user, reload(user, order.getOrderNumber()));
            if (shipment == null || !shipment.success()) {
                String reason = shipment == null
                        ? "Shipment is not available"
                        : shipment.message();
                return compensate(user, order, saga, payment, true, reason);
            }
            saga.setShipmentReserved(true);
            saga.setStep("COMPLETE");
            save(saga);

            var completed = checkout.completeSuccess(user.getId(), order.getOrderNumber(), payment);
            shipments.paymentResult(user, completed.order(), true);
            String emailStatus = checkout.notifyOrder("PAYMENT_SUCCESS", user, completed.order(), completed.txn());
            saga.setStatus("COMPLETED");
            saga.setStep("DONE");
            save(saga);
            log.info("Checkout saga completed {} orderNumber={}",
                    RequestMdc.describe(user), order.getOrderNumber());
            return checkout.toPaymentResponse(true, completed.order(), completed.txn(),
                    completed.message() + " " + checkout.emailPhrase(emailStatus, user, completed.order()),
                    emailStatus, payment);
        } catch (Exception ex) {
            log.error("Checkout saga failed {} orderNumber={}",
                    RequestMdc.describe(user), order.getOrderNumber(), ex);
            return compensate(user, order, saga, payment, saga.isPaymentCaptured(),
                    ex.getMessage() == null ? "Checkout could not be completed" : ex.getMessage());
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void recoverStuck() {
        Instant cutoff = Instant.now().minus(2, ChronoUnit.MINUTES);
        for (CheckoutSaga saga : sagas.findByStatusInAndUpdatedAtBefore(List.of("RUNNING", "COMPENSATING"), cutoff)) {
            try {
                User user = users.findById(saga.getUserId()).orElse(null);
                if (user == null) {
                    continue;
                }
                CustomerOrder order = orders.findByOrderNumberAndUserId(saga.getOrderNumber(), user.getId())
                        .orElse(null);
                if (order == null) {
                    continue;
                }
                log.warn("Checkout saga recovering stuck orderNumber={} step={}",
                        saga.getOrderNumber(), saga.getStep());
                compensate(user, order, saga, null, saga.isPaymentCaptured(),
                        saga.getFailureReason() == null
                                ? "Checkout timed out and was rolled back" : saga.getFailureReason());
            } catch (Exception ex) {
                log.error("Checkout saga recovery failed orderNumber={}", saga.getOrderNumber(), ex);
            }
        }
    }

    private PaymentResponse compensate(
            User user,
            CustomerOrder order,
            CheckoutSaga saga,
            PaymentResult payment,
            boolean refundMoney,
            String reason) {
        if (saga.isCompensated()) {
            var failed = checkout.failLocal(user.getId(), order.getOrderNumber(), reason, true);
            return checkout.toPaymentResponse(false, failed.order(), failed.txn(), failed.message(), "FAILED", payment);
        }
        saga.setStatus("COMPENSATING");
        saga.setFailureReason(trim(reason));
        save(saga);

        PaymentResult refund = payment;
        if (refundMoney && !saga.isCompensated()) {
            PaymentCommand command = checkout.paymentCommand(user, reload(user, order.getOrderNumber()), null,
                    order.getPayment() == null ? null : order.getPayment().getPaymentMode());
            refund = payments.refund(command);
            if (refund != null && refund.success()) {
                checkout.markRefunded(user.getId(), order.getOrderNumber(), refund);
            } else {
                log.error("Checkout saga refund failed orderNumber={} message={}",
                        order.getOrderNumber(), refund == null ? "empty" : refund.message());
            }
        }

        shipments.cancel(user, reload(user, order.getOrderNumber()));
        var failed = checkout.failLocal(user.getId(), order.getOrderNumber(), reason, refundMoney);
        String emailStatus = checkout.notifyOrder("PAYMENT_FAILURE", user, failed.order(), failed.txn());
        saga.setCompensated(true);
        saga.setStockReserved(false);
        saga.setPaymentCaptured(false);
        saga.setShipmentReserved(false);
        saga.setStatus("COMPENSATED");
        saga.setStep("COMPENSATED");
        save(saga);
        String message = trim(reason) + (refundMoney
                ? " Order cancelled and the charged amount was refunded to your account."
                : " Order cancelled. No charge was kept.");
        log.warn("Checkout saga compensated {} orderNumber={} reason={}",
                RequestMdc.describe(user), order.getOrderNumber(), reason);
        PaymentResult result = refund != null ? refund : payment;
        return checkout.toPaymentResponse(false, failed.order(), failed.txn(),
                message + " " + checkout.emailPhrase(emailStatus, user, failed.order()),
                emailStatus, result);
    }

    private CheckoutSaga start(User user, CustomerOrder order) {
        CheckoutSaga saga = sagas.findByOrderNumber(order.getOrderNumber()).orElseGet(CheckoutSaga::new);
        saga.setOrderNumber(order.getOrderNumber());
        saga.setUserId(user.getId());
        saga.setCustomerEmail(user.getEmail());
        saga.setCustomerName(user.getName());
        saga.setTransactionRef(order.getPayment() == null ? null : order.getPayment().getTransactionRef());
        saga.setPaymentMode(order.getPayment() == null || order.getPayment().getPaymentMode() == null
                ? null : order.getPayment().getPaymentMode().name());
        saga.setAmount(order.getTotal());
        saga.setStatus("RUNNING");
        saga.setStep("STOCK");
        saga.setStockReserved(true);
        saga.setPaymentCaptured(false);
        saga.setShipmentReserved(false);
        saga.setCompensated(false);
        return save(saga);
    }

    private CheckoutSaga save(CheckoutSaga saga) {
        saga.setUpdatedAt(Instant.now());
        return sagas.saveAndFlush(saga);
    }

    private CustomerOrder reload(User user, String orderNumber) {
        return orders.findByOrderNumberAndUserId(orderNumber, user.getId()).orElseThrow();
    }

    private static String trim(String value) {
        if (value == null || value.isBlank()) {
            return "Checkout was rolled back";
        }
        return value.length() > 480 ? value.substring(0, 480) : value;
    }
}
