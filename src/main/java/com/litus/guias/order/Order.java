package com.litus.guias.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class Order {

    private long id;
    private OrderPaymentCondition paymentCondition;
    private LocalDateTime createdAt;
    private List<OrderItem> items;
    private OrderStatus status;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private Long replacementOrderId;

    public Order(
            long id,
            OrderPaymentCondition paymentCondition,
            LocalDateTime createdAt,
            List<OrderItem> items
    ) {
        this(id, paymentCondition, createdAt, items, OrderStatus.ACTIVE, null, null, null);
    }

    public Order(
            long id,
            OrderPaymentCondition paymentCondition,
            LocalDateTime createdAt,
            List<OrderItem> items,
            OrderStatus status,
            String cancellationReason,
            LocalDateTime cancelledAt,
            Long replacementOrderId
    ) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one item"
            );
        }
        if (paymentCondition == null) throw new IllegalArgumentException("Order payment condition is required");
        if (createdAt == null) throw new IllegalArgumentException("Order date is required");
        if (status == null) throw new IllegalArgumentException("Order status is required");

        this.id = id;
        this.paymentCondition = paymentCondition;
        this.createdAt = createdAt;
        this.items = List.copyOf(items);
        this.status = status;
        this.cancellationReason = cancellationReason;
        this.cancelledAt = cancelledAt;
        this.replacementOrderId = replacementOrderId;
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

    public boolean hasPendingCost() {
        return items.stream().anyMatch(item -> item.getUnitCost().signum() == 0);
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Long getReplacementOrderId() {
        return replacementOrderId;
    }

    public void cancel(String reason, LocalDateTime at) {
        finish(OrderStatus.CANCELLED, reason, at, null);
    }

    public void markCorrected(String reason, LocalDateTime at, long replacementId) {
        finish(OrderStatus.CORRECTED, reason, at, replacementId);
    }

    private void finish(OrderStatus newStatus, String reason, LocalDateTime at, Long replacementId) {
        if (status != OrderStatus.ACTIVE) throw new IllegalStateException("El pedido ya fue modificado");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
        if (at == null) throw new IllegalArgumentException("La fecha es obligatoria");
        status = newStatus;
        cancellationReason = reason.trim();
        cancelledAt = at;
        replacementOrderId = replacementId;
    }
}
