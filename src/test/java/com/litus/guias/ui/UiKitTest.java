package com.litus.guias.ui;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiKitTest {
    @Test
    void acceptsZeroForOptionalUnitCosts() {
        assertEquals(0, UiKit.nonNegativeDecimal("0", "El costo").compareTo(BigDecimal.ZERO));
        assertEquals(0, UiKit.nonNegativeDecimal("18,50", "El costo")
                .compareTo(new BigDecimal("18.50")));
    }

    @Test
    void rejectsNegativeOrBlankUnitCosts() {
        assertThrows(IllegalArgumentException.class, () -> UiKit.nonNegativeDecimal("-1", "El costo"));
        assertThrows(IllegalArgumentException.class, () -> UiKit.nonNegativeDecimal("", "El costo"));
    }
}
