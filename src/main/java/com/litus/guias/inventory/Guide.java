package com.litus.guias.inventory;

import java.math.BigDecimal;

public class Guide {
    private long id;
    private String name;
    private BigDecimal currentPrice;
    private BigDecimal defaultUnitCost;
    private int stock;

    public Guide(long id, String name, BigDecimal currentPrice, int stock) {
        this(id, name, currentPrice, BigDecimal.ZERO, stock);
    }

    public Guide(long id, String name, BigDecimal currentPrice, BigDecimal defaultUnitCost, int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        if (defaultUnitCost == null || defaultUnitCost.signum() < 0) {
            throw new IllegalArgumentException("Default unit cost cannot be negative");
        }

        this.id = id;
        this.name = name;
        this.currentPrice = currentPrice;
        this.defaultUnitCost = defaultUnitCost;
        this.stock = stock;
    }

    public void sellOne() {
        if (stock <= 0) {
            throw new IllegalStateException("There is no stock available");
        }

        this.stock--;
    }

    public void addOne() {
        stock++;
    }

    public void addStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        stock = Math.addExact(stock, quantity);
    }

    public void removeStock(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        if (stock < quantity) throw new IllegalStateException("No hay stock suficiente de " + name);
        stock -= quantity;
    }

    public String getName() {
        return name;
    }

    public int getStock() {
        return stock;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getDefaultUnitCost() {
        return defaultUnitCost;
    }

    public void setDefaultUnitCost(BigDecimal defaultUnitCost) {
        if (defaultUnitCost == null || defaultUnitCost.signum() < 0) {
            throw new IllegalArgumentException("Default unit cost cannot be negative");
        }
        this.defaultUnitCost = defaultUnitCost;
    }

    public long getId() {
        return id;
    }
}
