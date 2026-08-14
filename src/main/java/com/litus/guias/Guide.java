package com.litus.guias;

import java.math.BigDecimal;

public class Guide {
    private long id;
    private String name;
    private BigDecimal currentPrice;
    private int stock;

    public Guide(long id, String name, BigDecimal currentPrice, int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }

        this.id = id;
        this.name = name;
        this.currentPrice = currentPrice;
        this.stock = stock;
    }

    public void sellOne() {
        if (stock <= 0) {
            throw new IllegalStateException("There is no stock available");
        }

        this.stock--;
    }

    public int getStock() {
        return stock;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public long getId() {
        return id;
    }
}
