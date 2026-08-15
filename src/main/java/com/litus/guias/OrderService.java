package com.litus.guias;

import java.util.List;

public class OrderService {

    public void registerOrder(Order order, List<Guide> guides) {
        for (OrderItem item : order.getItems()) {
            for (Guide guide : guides) {
                if (guide.getId() == item.getGuideId()) {
                    guide.addStock(item.getQuantity());
                    break;
                }
            }
        }
    }
}
