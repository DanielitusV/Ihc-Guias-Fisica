package com.litus.guias.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Order {

    private long id;
    private OrderPaymentCondition paymentCondition;
    private LocalDateTime createdAt;
    private List<OrderItem> items;

    public Order(
            long id,
            OrderPaymentCondition paymentCondition,
            LocalDateTime createdAt,
            List<OrderItem> items
    ) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one item"
            );
        }

        this.id = id;
        this.paymentCondition = paymentCondition;
        this.createdAt = createdAt;
        this.items = items;
    }

    public BigDecimal getTotalCost() {
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItem item : items) {
            total = total.add(item.getSubtotal());
        }

        return total;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderPaymentCondition getPaymentCondition() {
        return paymentCondition;
    }
}
