package com.pharmacy.drugstore.service;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import com.pharmacy.drugstore.export.ExportFile;
import com.pharmacy.drugstore.export.ExportFormat;
import com.pharmacy.drugstore.export.OrderExportFactory;
import com.pharmacy.drugstore.payment.PaymentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Locale;

@Service
public class OrderExportService {
    private final CheckoutService checkoutService;
    private final OrderExportFactory exportFactory;

    public OrderExportService(CheckoutService checkoutService, OrderExportFactory exportFactory) {
        this.checkoutService = checkoutService;
        this.exportFactory = exportFactory;
    }

    @Transactional(readOnly = true)
    public ExportFile exportHistory(User user, String format, String status) {
        List<CustomerOrder> orders = checkoutService.history(user);
        PaymentStatus filter = parseStatus(status);
        if (filter != null) {
            orders = orders.stream().filter(order -> order.getStatus() == filter).toList();
        }
        return exportFactory.create(ExportFormat.from(format)).export(user, orders);
    }

    @Transactional(readOnly = true)
    public ExportFile exportOrder(User user, String orderNumber, String format) {
        CustomerOrder order = checkoutService.get(user, orderNumber);
        return exportFactory.create(ExportFormat.from(format)).export(user, List.of(order));
    }

    private PaymentStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status.trim())) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be ALL, SUCCESS, PENDING or FAILED");
        }
    }
}
