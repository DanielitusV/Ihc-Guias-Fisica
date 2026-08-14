package com.litus.guias;

public class SaleResult {

    private final Sale sale;
    private final AccountMovement movement;

    public SaleResult(Sale sale, AccountMovement movement) {
        this.sale = sale;
        this.movement = movement;
    }

    public Sale getSale() {
        return sale;
    }

    public AccountMovement getMovement() {
        return movement;
    }
}
