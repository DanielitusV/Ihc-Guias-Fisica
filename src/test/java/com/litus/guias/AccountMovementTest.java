package com.litus.guias;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountMovementTest {

    @Test
    public void expenseWithoutReasonIsNotAllowed() {
        assertThrows(IllegalArgumentException.class,
                () -> new AccountMovement(
                        0,
                        1,
                        AccountMovementType.EXPENSE,
                        AccountMovementConcept.GENERAL_EXPENSE,
                        new BigDecimal("20.00"),
                        "",
                        LocalDateTime.of(2026, 8, 14, 19, 30)
                )
        );
    }

    @Test
    public void movementStoresItsData() {
        LocalDateTime date = LocalDateTime.of(2026, 8, 14, 19, 30);

        AccountMovement movement = new AccountMovement(
                1,
                2,
                AccountMovementType.EXPENSE,
                AccountMovementConcept.GENERAL_EXPENSE,
                new BigDecimal("20.00"),
                "Agua",
                date
        );

        assertEquals(new BigDecimal("20.00"), movement.getAmount());
        assertEquals(AccountMovementConcept.GENERAL_EXPENSE, movement.getConcept());
        assertEquals("Agua", movement.getReason());
        assertEquals(date, movement.getCreatedAt());
    }

    @Test
    public void movementAmountMustBePositive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AccountMovement(
                        1,
                        2,
                        AccountMovementType.EXPENSE,
                        AccountMovementConcept.GENERAL_EXPENSE,
                        BigDecimal.ZERO,
                        "Agua",
                        LocalDateTime.of(2026, 8, 14, 19, 30)
                )
        );
    }
}
