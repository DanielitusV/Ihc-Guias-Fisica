package com.litus.guias.persistence;

import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderPaymentCondition;
import com.litus.guias.order.OrderStatus;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {

    private final Database database;

    public OrderRepository(Database database) {
        this.database = database;
    }

    public long save(Order order) throws Exception {
        return database.inTransaction(
                connection -> save(connection, order)
        );
    }

    public long save(
            Connection connection,
            Order order
    ) throws Exception {

        String orderSql = """
                INSERT INTO orders
                (payment_condition, created_at, academic_term_id, status)
                VALUES (?, ?, ?, ?)
                """;

        long orderId;

        try (var statement = connection.prepareStatement(
                orderSql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setString(
                    1,
                    order.getPaymentCondition().name()
            );

            statement.setString(
                    2,
                    order.getCreatedAt().toString()
            );
            statement.setLong(3, AcademicTermRepository.requireActiveId(connection));
            statement.setString(4, order.getStatus().name());

            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException(
                            "Could not generate order ID"
                    );
                }

                orderId = keys.getLong(1);
            }
        }

        String itemSql = """
                INSERT INTO order_items
                (order_id, guide_id, quantity, unit_cost)
                VALUES (?, ?, ?, ?)
                """;

        for (OrderItem item : order.getItems()) {
            try (var statement =
                         connection.prepareStatement(itemSql)) {

                statement.setLong(1, orderId);
                statement.setLong(2, item.getGuideId());
                statement.setInt(3, item.getQuantity());
                statement.setBigDecimal(
                        4,
                        item.getUnitCost()
                );

                statement.executeUpdate();
            }
        }

        return orderId;
    }

    public Order findById(long id) throws Exception {
        try (Connection connection =
                     database.getConnection()) {

            return findById(connection, id);
        }
    }

    public Order findById(
            Connection connection,
            long id
    ) throws Exception {

        String orderSql = """
                SELECT
                    id,
                    payment_condition,
                    created_at,
                    status,
                    cancellation_reason,
                    cancelled_at,
                    replacement_order_id
                FROM orders
                WHERE id = ?
                """;

        OrderPaymentCondition paymentCondition;
        LocalDateTime createdAt;
        OrderStatus status;
        String cancellationReason;
        LocalDateTime cancelledAt;
        Long replacementOrderId;

        try (var statement =
                     connection.prepareStatement(orderSql)) {

            statement.setLong(1, id);

            try (var result = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                paymentCondition =
                        OrderPaymentCondition.valueOf(
                                result.getString(
                                        "payment_condition"
                                )
                        );

                createdAt =
                        LocalDateTime.parse(
                                result.getString(
                                        "created_at"
                                )
                        );
                status = OrderStatus.valueOf(result.getString("status"));
                cancellationReason = result.getString("cancellation_reason");
                String cancelled = result.getString("cancelled_at");
                cancelledAt = cancelled == null ? null : LocalDateTime.parse(cancelled);
                long replacement = result.getLong("replacement_order_id");
                replacementOrderId = result.wasNull() ? null : replacement;
            }
        }

        String itemSql = """
                SELECT
                    id,
                    order_id,
                    guide_id,
                    quantity,
                    unit_cost
                FROM order_items
                WHERE order_id = ?
                """;

        List<OrderItem> items =
                new ArrayList<>();

        try (var statement =
                     connection.prepareStatement(itemSql)) {

            statement.setLong(1, id);

            try (var result = statement.executeQuery()) {

                while (result.next()) {
                    items.add(
                            new OrderItem(
                                    result.getLong("id"),
                                    result.getLong("order_id"),
                                    result.getLong("guide_id"),
                                    result.getInt("quantity"),
                                    result.getBigDecimal(
                                            "unit_cost"
                                    )
                            )
                    );
                }
            }
        }

        return new Order(
                id,
                paymentCondition,
                createdAt,
                items,
                status,
                cancellationReason,
                cancelledAt,
                replacementOrderId
        );
    }

    public List<Order> findAll() throws Exception {
        try (Connection connection =
                     database.getConnection()) {

            return findAll(connection);
        }
    }

    public List<Order> findAll(
            Connection connection
    ) throws Exception {
        Long activeId = AcademicTermRepository.activeIdOrNull(connection);
        return activeId == null ? List.of() : findAllByTerm(connection, activeId);
    }

    public List<Order> findAllByTerm(long academicTermId) throws Exception {
        try (Connection connection = database.getConnection()) {
            return findAllByTerm(connection, academicTermId);
        }
    }

    private List<Order> findAllByTerm(Connection connection, long academicTermId) throws Exception {

        String orderSql = """
                SELECT id, payment_condition, created_at, status,
                       cancellation_reason, cancelled_at, replacement_order_id
                FROM orders
                WHERE academic_term_id = ?
                ORDER BY created_at ASC, id ASC
                """;

        Map<Long, OrderPaymentCondition> conditions = new LinkedHashMap<>();
        Map<Long, LocalDateTime> dates = new LinkedHashMap<>();
        Map<Long, List<OrderItem>> itemsByOrder = new LinkedHashMap<>();
        Map<Long, OrderStatus> statuses = new LinkedHashMap<>();
        Map<Long, String> cancellationReasons = new LinkedHashMap<>();
        Map<Long, LocalDateTime> cancellationDates = new LinkedHashMap<>();
        Map<Long, Long> replacements = new LinkedHashMap<>();

        try (var statement = connection.prepareStatement(orderSql)) {
            statement.setLong(1, academicTermId);
            try (var result = statement.executeQuery()) {

                while (result.next()) {
                    long id = result.getLong("id");
                    conditions.put(
                        id,
                        OrderPaymentCondition.valueOf(
                                result.getString("payment_condition")
                        )
                );
                    dates.put(
                        id,
                        LocalDateTime.parse(result.getString("created_at"))
                );
                    itemsByOrder.put(id, new ArrayList<>());
                    statuses.put(id, OrderStatus.valueOf(result.getString("status")));
                    cancellationReasons.put(id, result.getString("cancellation_reason"));
                    String cancelled = result.getString("cancelled_at");
                    cancellationDates.put(id, cancelled == null ? null : LocalDateTime.parse(cancelled));
                    long replacement = result.getLong("replacement_order_id");
                    replacements.put(id, result.wasNull() ? null : replacement);
                }
            }
        }

        String itemSql = """
                SELECT id, order_id, guide_id, quantity, unit_cost
                FROM order_items
                ORDER BY order_id ASC, id ASC
                """;
        try (var statement = connection.prepareStatement(itemSql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                List<OrderItem> items = itemsByOrder.get(
                        result.getLong("order_id")
                );
                if (items != null) {
                    items.add(new OrderItem(
                            result.getLong("id"),
                            result.getLong("order_id"),
                            result.getLong("guide_id"),
                            result.getInt("quantity"),
                            result.getBigDecimal("unit_cost")
                    ));
                }
            }
        }

        List<Order> orders = new ArrayList<>();
        for (Long id : conditions.keySet()) {
            orders.add(new Order(
                    id,
                    conditions.get(id),
                    dates.get(id),
                    itemsByOrder.get(id),
                    statuses.get(id),
                    cancellationReasons.get(id),
                    cancellationDates.get(id),
                    replacements.get(id)
            ));
        }

        return orders;
    }

    public void updateStatus(Connection connection, Order order) throws Exception {
        String sql = """
                UPDATE orders
                SET status = ?, cancellation_reason = ?, cancelled_at = ?, replacement_order_id = ?
                WHERE id = ? AND status = 'ACTIVE'
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, order.getStatus().name());
            statement.setString(2, order.getCancellationReason());
            statement.setString(3, order.getCancelledAt() == null ? null : order.getCancelledAt().toString());
            if (order.getReplacementOrderId() == null) statement.setNull(4, java.sql.Types.INTEGER);
            else statement.setLong(4, order.getReplacementOrderId());
            statement.setLong(5, order.getId());
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("No se pudo modificar el pedido; quizá ya fue corregido o anulado");
            }
        }
    }
}
