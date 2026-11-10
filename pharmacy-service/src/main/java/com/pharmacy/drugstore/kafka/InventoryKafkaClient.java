package com.pharmacy.drugstore.kafka;

import com.pharmacy.drugstore.entity.CustomerOrder;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class InventoryKafkaClient {
    private static final Logger log = LoggerFactory.getLogger(InventoryKafkaClient.class);
    private final ReplyingKafkaTemplate<String, Object, StockResult> replies;
    private final String commandsTopic;

    public InventoryKafkaClient(
            ReplyingKafkaTemplate<String, Object, StockResult> stockReplyKafkaTemplate,
            @Value("${app.kafka.topics.stock-commands}") String commandsTopic) {
        this.replies = stockReplyKafkaTemplate;
        this.commandsTopic = commandsTopic;
    }

    public StockResult reserve(CustomerOrder order) {
        return send("RESERVE", order);
    }

    public StockResult release(CustomerOrder order) {
        return send("RELEASE", order);
    }

    private StockResult send(String action, CustomerOrder order) {
        if (order == null) {
            return new StockResult(null, null, false, "Order is required");
        }
        List<StockCommand.StockLine> items = order.getItems() == null ? List.of() : order.getItems().stream()
                .map(item -> new StockCommand.StockLine(item.getProductId(), item.getQuantity(), item.getProductName()))
                .toList();
        StockCommand command = new StockCommand(action, UUID.randomUUID().toString(), order.getOrderNumber(), items);
        log.info("Stock command published action={} orderNumber={}", action, order.getOrderNumber());
        ProducerRecord<String, Object> record = new ProducerRecord<>(commandsTopic, order.getOrderNumber(), command);
        try {
            RequestReplyFuture<String, Object, StockResult> future = replies.sendAndReceive(record);
            StockResult result = future.get(20, TimeUnit.SECONDS).value();
            if (result == null) {
                return new StockResult(command.correlationId(), order.getOrderNumber(), false, "Inventory returned an empty result");
            }
            log.info("Stock result received action={} orderNumber={} success={}",
                    action, order.getOrderNumber(), result.success());
            return result;
        } catch (Exception ex) {
            log.error("Stock kafka request failed action={} orderNumber={}", action, order.getOrderNumber(), ex);
            return new StockResult(command.correlationId(), order.getOrderNumber(), false,
                    "Inventory is unavailable. Start inventory-service on port 8088.");
        }
    }
}
