package com.litus.guias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExpenseService {

    public AccountMovement registerExpense(
            Account account,
            BigDecimal amount,
            String reason,
            LocalDateTime createdAt
    ) {
        AccountMovement movement = new AccountMovement(
                0,
                account.getId(),
                AccountMovementType.EXPENSE,
                AccountMovementConcept.GENERAL_EXPENSE,
                amount,
                reason,
                createdAt
        );

        account.addExpense(amount);
        return movement;
    }
}
