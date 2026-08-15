package com.litus.guias.account;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplierPaymentServiceTest {

    @Test
    public void supplierPaymentReducesAccountBalanceAndCreatesMovement() {
        Account cashAccount = new Account(
                1,
                "Efectivo",
                new BigDecimal("500.00")
        );

        SupplierPaymentService service = new SupplierPaymentService();

        AccountMovement movement = service.registerPayment(
                cashAccount,
                new BigDecimal("150.00"),
                "Pago parcial a fotocopiadora",
                LocalDateTime.of(2026, 8, 14, 21, 0)
        );

        assertEquals(new BigDecimal("350.00"), cashAccount.getBalance());
        assertEquals(AccountMovementType.EXPENSE, movement.getType());
        assertEquals(AccountMovementConcept.SUPPLIER_PAYMENT, movement.getConcept());
        assertEquals(new BigDecimal("150.00"), movement.getAmount());
        assertEquals(1, movement.getAccountId());
    }

    @Test
    public void invalidSupplierPaymentDoesNotChangeAccountBalance() {
        Account cashAccount = new Account(
                1,
                "Efectivo",
                new BigDecimal("500.00")
        );

        SupplierPaymentService service = new SupplierPaymentService();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registerPayment(
                        cashAccount,
                        new BigDecimal("150.00"),
                        "",
                        LocalDateTime.of(2026, 8, 14, 21, 0)
                )
        );

        assertEquals(new BigDecimal("500.00"), cashAccount.getBalance());
    }
}
