package com.litus.guias;

import java.math.BigDecimal;

public class Account {

    private long id;
    private String name;
    private BigDecimal balance;

    public Account(long id, String name, BigDecimal balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    public void addIncome(BigDecimal amount) {
        this.balance = balance.add(amount);
    }

    public BigDecimal getBalance() {
        return this.balance;
    }
}
