package com.pharmacy.drugstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ProductVersionBackfill implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(ProductVersionBackfill.class);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private volatile boolean running;

    public ProductVersionBackfill(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.tx = new TransactionTemplate(transactions);
    }

    @Override
    public void start() {
        tx.executeWithoutResult(status -> {
            backfill();
            widenOutboxEventId();
        });
        running = true;
    }

    private void widenOutboxEventId() {
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pharmacy_outbox_events'",
                Integer.class);
        if (tables == null || tables == 0) {
            return;
        }
        Integer length = jdbc.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pharmacy_outbox_events' "
                        + "AND COLUMN_NAME = 'event_id'",
                Integer.class);
        if (length != null && length >= 128) {
            return;
        }
        jdbc.execute("ALTER TABLE pharmacy_outbox_events MODIFY COLUMN event_id VARCHAR(128) NOT NULL");
        log.info("Widened pharmacy_outbox_events.event_id to VARCHAR(128)");
    }

    private void backfill() {
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
        if (filled > 0) {
            log.info("Backfilled products.version rows={}", filled);
        }
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
