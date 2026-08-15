package com.litus.guias.order;

import com.litus.guias.inventory.Guide;

import java.util.List;

public class OrderService {

    public void registerOrder(Order order, List<Guide> guides) {
        // 1. Validar que todas las guías existan
        for (OrderItem item : order.getItems()) {
            findGuide(item.getGuideId(), guides);
        }

        // 2. Solo las guías son válidas, se modifica el stock
        for (OrderItem item : order.getItems()) {
            Guide guide = findGuide(item.getGuideId(), guides);
            guide.addStock(item.getQuantity());
        }
    }

    private Guide findGuide (long guideId, List<Guide> guides) {
        for (Guide guide: guides) {
            if (guide.getId() == guideId) {
                return guide;
            }
        }

        throw new IllegalArgumentException(
                "Guide not found: " + guideId
        );
    }
}
