package com.litus.guias;

import java.math.BigDecimal;

public class OrderItem {

    private long id;
    private long orderId;
    private long guideId;
    private int quantity;
    private BigDecimal unitCost;

    public OrderItem(
            long id,
            long orderId,
            long guideId,
            int quantity,
            BigDecimal unitCost
    ) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (unitCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit cost must be greater than zero");
        }

        this.id = id;
        this.orderId = orderId;
        this.guideId = guideId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }

    public long getGuideId() {
        return this.guideId;
    }

    public int getQuantity() {
        return this.quantity;
    }

    public BigDecimal getSubtotal() {
        return unitCost.multiply(new BigDecimal(quantity));
    }
}
