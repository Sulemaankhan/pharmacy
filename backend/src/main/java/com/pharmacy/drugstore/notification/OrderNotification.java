package com.pharmacy.drugstore.notification;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.PaymentTransaction;
import com.pharmacy.drugstore.entity.User;

public interface OrderNotification {
    void send(User user, CustomerOrder order, PaymentTransaction transaction);
}
