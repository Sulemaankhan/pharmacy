package com.pharmacy.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class InventorySchemaInitializer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(InventorySchemaInitializer.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private volatile boolean running;

    public InventorySchemaInitializer(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactions);
    }

    @Override
    public void start() {
        tx.executeWithoutResult(status -> {
            jdbc.execute(
                    "CREATE TABLE IF NOT EXISTS stock_reservations ("
                            + "id BIGINT NOT NULL AUTO_INCREMENT,"
                            + "order_number VARCHAR(255) NOT NULL,"
                            + "status VARCHAR(255) NOT NULL,"
                            + "reservation_lines VARCHAR(2000),"
                            + "PRIMARY KEY (id),"
                            + "UNIQUE KEY uk_stock_reservations_order_number (order_number)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
            );
            log.info("Inventory schema ready table=stock_reservations");
        });
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
