package com.litus.guias;

import java.math.BigDecimal;

public class Sale {

    private long id;
    private long guideId;
    private BigDecimal price;
    private PaymentMethod paymentMethod;

    public Sale(long id, long guideId, BigDecimal price,
                PaymentMethod paymentMethod) {
        this.id = id;
        this.guideId = guideId;
        this.price = price;
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public PaymentMethod getPaymentMethod() {
        return this.paymentMethod;
    }
}
