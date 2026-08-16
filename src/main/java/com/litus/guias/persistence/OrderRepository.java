package com.litus.guias.persistence;

import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderPaymentCondition;

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
                (payment_condition, created_at)
                VALUES (?, ?)
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
                    created_at
                FROM orders
                WHERE id = ?
                """;

        OrderPaymentCondition paymentCondition;
        LocalDateTime createdAt;

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
                items
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

        String orderSql = """
                SELECT id, payment_condition, created_at
                FROM orders
                ORDER BY created_at ASC, id ASC
                """;

        Map<Long, OrderPaymentCondition> conditions = new LinkedHashMap<>();
        Map<Long, LocalDateTime> dates = new LinkedHashMap<>();
        Map<Long, List<OrderItem>> itemsByOrder = new LinkedHashMap<>();

        try (var statement =
                     connection.prepareStatement(orderSql);
             var result = statement.executeQuery()) {

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
                    itemsByOrder.get(id)
            ));
        }

        return orders;
    }
}
