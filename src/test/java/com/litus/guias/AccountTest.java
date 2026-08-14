package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

public class AccountTest {

    @Test
    public void incomeIncreasesBalance() {
        Account account = new Account(
                1,
                "Efectivo",
                new BigDecimal("100.00")
        );

        account.addIncome(new BigDecimal("25.00"));

        assertEquals(
                new BigDecimal("125.00"),
                account.getBalance()
        );
    }

    @Test
    public void expenseDecreasesBalance() {
        Account account = new Account(
                1,
                "Efectivo",
                new BigDecimal("100.00")
        );

        account.addExpense(new BigDecimal("30.00"));

        assertEquals(
                new BigDecimal("70.00"),
                account.getBalance()
        );
    }
}
