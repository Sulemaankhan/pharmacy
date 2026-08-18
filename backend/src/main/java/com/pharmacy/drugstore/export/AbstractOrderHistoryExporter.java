package com.pharmacy.drugstore.export;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.OrderItem;
import com.pharmacy.drugstore.entity.User;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractOrderHistoryExporter implements OrderHistoryExporter {
    protected static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    protected static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(IST);

    @Override
    public final ExportFile export(User user, List<CustomerOrder> orders) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }
        return write(user, orders == null ? List.of() : orders);
    }

    protected abstract ExportFile write(User user, List<CustomerOrder> orders);

    protected String itemsSummary(CustomerOrder order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "-";
        }
        return order.getItems().stream()
                .map(item -> item.getProductName() + " x" + item.getQuantity())
                .collect(Collectors.joining(", "));
    }

    protected String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.toPlainString();
    }

    protected String paymentMode(CustomerOrder order) {
        return order.getPayment() == null || order.getPayment().getPaymentMode() == null
                ? "-"
                : order.getPayment().getPaymentMode().name();
    }

    protected String txnRef(CustomerOrder order) {
        return order.getPayment() == null || order.getPayment().getTransactionRef() == null
                ? "-"
                : order.getPayment().getTransactionRef();
    }

    protected String fileStamp() {
        return DateTimeFormatter.ofPattern("yyyyMMdd-HHmm").withZone(IST).format(java.time.Instant.now());
    }

    protected BigDecimal lineAmount(OrderItem item) {
        if (item.getUnitPrice() == null) {
            return BigDecimal.ZERO;
        }
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
