package com.litus.guias;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplierPaymentService {

    public AccountMovement registerPayment(
            Account account,
            BigDecimal amount,
            String reason,
            LocalDateTime createdAt
    ) {
        AccountMovement movement = new AccountMovement(
                0,
                account.getId(),
                AccountMovementType.EXPENSE,
                AccountMovementConcept.SUPPLIER_PAYMENT,
                amount,
                reason,
                createdAt
        );

        account.addExpense(amount);
        return movement;
    }
}
