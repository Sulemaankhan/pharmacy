package com.pharmacy.drugstore.saga;

import com.pharmacy.drugstore.dto.PaymentResponse;
import com.pharmacy.drugstore.kafka.DomainEvent;
import com.pharmacy.drugstore.reliability.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class SagaEventListener {
    private static final Logger log = LoggerFactory.getLogger(SagaEventListener.class);
    private static final String CONSUMER = "pharmacy-saga-orchestrator";

    private final CheckoutSagaOrchestrator saga;
    private final IdempotencyService idempotency;
    private final SagaWaitRegistry waits;

    public SagaEventListener(
            CheckoutSagaOrchestrator saga,
            IdempotencyService idempotency,
            SagaWaitRegistry waits) {
        this.saga = saga;
        this.idempotency = idempotency;
        this.waits = waits;
    }

    @KafkaListener(
            topics = {
                    "${app.kafka.topics.inventory-events}",
                    "${app.kafka.topics.payment-events}",
                    "${app.kafka.topics.order-events}",
                    "${app.kafka.topics.fulfilment-events}",
                    "${app.kafka.topics.shipment-events}"
            },
            groupId = "pharmacy-saga-orchestrator",
            containerFactory = "sagaEventListenerFactory")
    @Transactional
    public void onEvent(DomainEvent event) {
        if (event == null || event.getEventType() == null || event.getOrderNumber() == null) {
            return;
        }
        if (!idempotency.markIfNew(CONSUMER, event.getEventId(), event.getEventType(), event.getOrderNumber())) {
            log.info("Saga ignored duplicate eventType={} orderNumber={}", event.getEventType(), event.getOrderNumber());
            return;
        }
        log.info("Saga received eventType={} orderNumber={}", event.getEventType(), event.getOrderNumber());
        switch (event.getEventType()) {
            case "INVENTORY_RESERVED" -> saga.onInventoryReserved(event);
            case "INVENTORY_FAILED" -> complete(event.getOrderNumber(), saga.onInventoryFailed(event));
            case "PAYMENT_SUCCESS" -> complete(event.getOrderNumber(), saga.onPaymentSuccess(event));
            case "PAYMENT_FAILED" -> complete(event.getOrderNumber(), saga.onPaymentFailed(event));
            case "ORDER_CONFIRMED" -> saga.onOrderConfirmed(event);
            case "SHIPMENT_CREATED" -> saga.onShipmentCreated(event);
            case "SHIPMENT_FAILED" -> saga.onShipmentFailed(event);
            case "SHIPMENT_PICKED_UP" -> saga.onShipmentPickedUp(event);
            case "SHIPMENT_DELIVERED" -> saga.onShipmentDelivered(event);
            default -> log.debug("Saga ignored eventType={} orderNumber={}",
                    event.getEventType(), event.getOrderNumber());
        }
    }

    private void complete(String orderNumber, PaymentResponse response) {
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
}
