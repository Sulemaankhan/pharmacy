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
            ensureProductVersion();
            log.info("Inventory schema ready table=stock_reservations");
        });
        running = true;
    }

    /**
     * Catalog rows were inserted before products.version existed, so the column is NULL.
     * Hibernate NPEs while incrementing @Version, the commit rolls back, and the stock command is retried forever.
     */
    private void ensureProductVersion() {
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products'",
                Integer.class);
        if (tables == null || tables == 0) {
            return;
        }
        Integer columns = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'products' AND COLUMN_NAME = 'version'",
                Integer.class);
        if (columns == null || columns == 0) {
            jdbc.execute("ALTER TABLE products ADD COLUMN version BIGINT NOT NULL DEFAULT 0");
            log.info("Added products.version default=0");
            return;
        }
        int filled = jdbc.update("UPDATE products SET version = 0 WHERE version IS NULL");
        jdbc.execute("ALTER TABLE products MODIFY COLUMN version BIGINT NOT NULL DEFAULT 0");
        log.info("Products version column ready backfilled={}", filled);
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
