package com.pharmacy.drugstore.export;


import java.util.List;
import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;

public sealed interface OrderHistoryExporter permits AbstractOrderHistoryExporter {
    ExportFormat format();
    ExportFile export(User user, List<CustomerOrder> orders);
}
