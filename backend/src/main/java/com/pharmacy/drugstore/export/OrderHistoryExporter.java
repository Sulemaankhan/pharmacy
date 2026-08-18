package com.pharmacy.drugstore.export;

import com.pharmacy.drugstore.entity.CustomerOrder;
import com.pharmacy.drugstore.entity.User;
import java.util.List;

public interface OrderHistoryExporter {
    ExportFormat format();
    ExportFile export(User user, List<CustomerOrder> orders);
}
