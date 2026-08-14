package com.litus.guias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Sale {

    private long id;
    private long guideId;
    private BigDecimal price;
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
    private SaleStatus status;

    public Sale(long id, long guideId, BigDecimal price,
                PaymentMethod paymentMethod, LocalDateTime createdAt,
                SaleStatus status) {
        this.id = id;
        this.guideId = guideId;
        this.price = price;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.status = status;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public PaymentMethod getPaymentMethod() {
        return this.paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public SaleStatus getStatus() {
        return this.status;
    }
}
