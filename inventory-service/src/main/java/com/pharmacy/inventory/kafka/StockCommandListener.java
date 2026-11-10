package com.pharmacy.inventory.kafka;

import com.pharmacy.inventory.event.StockCommand;
import com.pharmacy.inventory.event.StockResult;
import com.pharmacy.inventory.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

@Component
public class StockCommandListener {
    private static final Logger log = LoggerFactory.getLogger(StockCommandListener.class);
    private final StockService stock;

    public StockCommandListener(StockService stock) {
        this.stock = stock;
    }

    @KafkaListener(topics = "${app.kafka.topics.stock-commands}", groupId = "inventory-service")
    @SendTo("${app.kafka.topics.stock-replies}")
    public StockResult onCommand(StockCommand command) {
        log.info("Stock command received action={} orderNumber={}",
                command == null ? null : command.action(),
                command == null ? null : command.orderNumber());
        try {
            return stock.handle(command);
        } catch (Exception ex) {
            log.error("Stock command failed orderNumber={}", command == null ? null : command.orderNumber(), ex);
            return StockResult.fail(command, ex.getMessage() == null ? "Stock could not be updated" : ex.getMessage());
        }
    }
}
