CREATE TABLE IF NOT EXISTS stock_reservations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    reservation_lines VARCHAR(2000),
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_reservations_order_number (order_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
