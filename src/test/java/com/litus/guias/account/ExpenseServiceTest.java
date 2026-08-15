package com.litus.guias.account;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExpenseServiceTest {

    @Test
    public void registeringExpenseReducesBalanceAndCreatesMovement() {
        Account cashAccount = new Account(
                1,
                "Efectivo",
                new BigDecimal("500.00")
        );

        ExpenseService service = new ExpenseService();
        AccountMovement movement = service.registerExpense(
                cashAccount,
                new BigDecimal("40.00"),
                "Agua",
                LocalDateTime.of(2026, 8, 14, 21, 0)
        );

        assertEquals(new BigDecimal("460.00"), cashAccount.getBalance());
        assertEquals(AccountMovementType.EXPENSE, movement.getType());
        assertEquals(AccountMovementConcept.GENERAL_EXPENSE, movement.getConcept());
        assertEquals(new BigDecimal("40.00"), movement.getAmount());
        assertEquals("Agua", movement.getReason());
    }
}
