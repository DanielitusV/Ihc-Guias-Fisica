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
        validatePositiveAmount(amount);
        this.balance = balance.add(amount);
    }

    public void addExpense(BigDecimal amount) {
        validatePositiveAmount(amount);
        this.balance = balance.subtract(amount);
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    private void validatePositiveAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
