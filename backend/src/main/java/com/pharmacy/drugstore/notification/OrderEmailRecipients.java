package com.pharmacy.drugstore.notification;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import java.util.LinkedHashSet;
import java.util.List;

final class OrderEmailRecipients {
    private OrderEmailRecipients() {}

    static List<String> of(User user, CustomerOrder order) {
        LinkedHashSet<String> to = new LinkedHashSet<>();
        if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            to.add(user.getEmail().trim());
        }
        if (order != null && order.getShipment() != null && order.getShipment().getEmail() != null
                && !order.getShipment().getEmail().isBlank()) {
            to.add(order.getShipment().getEmail().trim());
        }
        return List.copyOf(to);
    }
}
