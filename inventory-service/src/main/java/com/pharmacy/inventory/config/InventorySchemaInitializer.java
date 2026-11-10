package com.pharmacy.inventory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventorySchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InventorySchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public InventorySchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
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
    }
}
