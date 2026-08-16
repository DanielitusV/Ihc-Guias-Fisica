package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderTransactionService {

    private final Database database;
    private final GuideRepository guideRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderTransactionService(Database database) {
        this.database = database;
        this.guideRepository = new GuideRepository(database);
        this.orderRepository = new OrderRepository(database);
        this.orderService = new OrderService();
    }

    public long registerOrder(Order order) throws Exception {
        return database.inTransaction(connection -> {

            Map<Long, Guide> guidesById = new LinkedHashMap<>();

            for (OrderItem item : order.getItems()) {
                Guide guide = guideRepository.findById(
                        connection,
                        item.getGuideId()
                );

                if (guide == null) {
                    throw new IllegalArgumentException(
                            "Guide not found: " + item.getGuideId()
                    );
                }

                guidesById.putIfAbsent(guide.getId(), guide);
            }

            ArrayList<Guide> guides =
                    new ArrayList<>(guidesById.values());

            orderService.registerOrder(order, guides);

            long orderId =
                    orderRepository.save(connection, order);

            for (Guide guide : guides) {
                guideRepository.update(connection, guide);
            }

            return orderId;
        });
    }
}