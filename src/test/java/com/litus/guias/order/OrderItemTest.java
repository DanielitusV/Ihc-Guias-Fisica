package com.litus.guias.order;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

public class OrderItemTest {

    @Test
    public void quantityMustBeGreaterThanZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new OrderItem(
                        0,
                        1,
                        2,
                        0,
                        new BigDecimal("23.20")
                )
        );
    }

    @Test
    public void zeroUnitCostIsAllowedWhilePriceIsPending() {
        assertDoesNotThrow(() -> new OrderItem(
                0, 1, 2, 10, BigDecimal.ZERO));
    }

    @Test
    public void unitCostCannotBeNegative() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OrderItem(
                        0,
                        1,
                        2,
                        10,
                        new BigDecimal("-0.01")
                )
        );
    }
}
