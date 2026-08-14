package com.litus.guias;

import java.math.BigDecimal;

public class Sale {

    private long id;
    private long guideId;
    private BigDecimal price;

    public Sale(long id, long guideId, BigDecimal price) {
        this.id = id;
        this.guideId = guideId;
        this.price = price;
    }

    public BigDecimal getPrice() {
        return this.price;
    }
}
