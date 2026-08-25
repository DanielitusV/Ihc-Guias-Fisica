package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderService;
import com.litus.guias.order.OrderStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderTransactionService {

    private final Database database;
    private final GuideRepository guideRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final SupplierDebtQueryService supplierDebt;

    public OrderTransactionService(Database database) {
        this.database = database;
        this.guideRepository = new GuideRepository(database);
        this.orderRepository = new OrderRepository(database);
        this.orderService = new OrderService();
        this.supplierDebt = new SupplierDebtQueryService(database);
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

    public void cancelOrder(long orderId, String reason, java.time.LocalDateTime cancelledAt)
            throws Exception {
        database.inTransaction(connection -> {
            Order order = requireActiveOrder(connection, orderId);
            Map<Long, Integer> quantities = quantities(order);
            for (var entry : quantities.entrySet()) {
                Guide guide = requireGuide(connection, entry.getKey());
                guide.removeStock(entry.getValue());
                guideRepository.update(connection, guide);
            }
            order.cancel(reason, cancelledAt);
            orderRepository.updateStatus(connection, order);
            supplierDebt.calculateCurrentDebt(connection);
            return null;
        });
    }

    public long correctOrder(long orderId, Order replacement, String reason,
            java.time.LocalDateTime correctedAt) throws Exception {
        if (replacement == null) throw new IllegalArgumentException("El pedido corregido es obligatorio");
        if (replacement.getStatus() != OrderStatus.ACTIVE) {
            throw new IllegalArgumentException("El pedido corregido debe estar activo");
        }
        return database.inTransaction(connection -> {
            Order original = requireActiveOrder(connection, orderId);
            Map<Long, Integer> originalQuantities = quantities(original);
            Map<Long, Integer> replacementQuantities = quantities(replacement);
            java.util.LinkedHashSet<Long> guideIds = new java.util.LinkedHashSet<>();
            guideIds.addAll(originalQuantities.keySet());
            guideIds.addAll(replacementQuantities.keySet());

            Map<Long, Guide> changedGuides = new LinkedHashMap<>();
            for (Long guideId : guideIds) {
                Guide guide = requireGuide(connection, guideId);
                int delta = replacementQuantities.getOrDefault(guideId, 0)
                        - originalQuantities.getOrDefault(guideId, 0);
                if (delta > 0) guide.addStock(delta);
                else if (delta < 0) guide.removeStock(Math.negateExact(delta));
                changedGuides.put(guideId, guide);
            }

            long replacementId = orderRepository.save(connection, replacement);
            original.markCorrected(reason, correctedAt, replacementId);
            orderRepository.updateStatus(connection, original);
            supplierDebt.calculateCurrentDebt(connection);
            for (Guide guide : changedGuides.values()) guideRepository.update(connection, guide);
            return replacementId;
        });
    }

    private Order requireActiveOrder(java.sql.Connection connection, long orderId) throws Exception {
        Order order = orderRepository.findById(connection, orderId);
        if (order == null) throw new IllegalArgumentException("No se encontró el pedido");
        if (order.getStatus() != OrderStatus.ACTIVE) {
            throw new IllegalStateException("El pedido ya fue corregido o anulado");
        }
        return order;
    }

    private Guide requireGuide(java.sql.Connection connection, long guideId) throws Exception {
        Guide guide = guideRepository.findById(connection, guideId);
        if (guide == null) throw new IllegalArgumentException("No se encontró una guía del pedido");
        return guide;
    }

    private Map<Long, Integer> quantities(Order order) {
        Map<Long, Integer> values = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            values.merge(item.getGuideId(), item.getQuantity(), Integer::sum);
        }
        return values;
    }
}
