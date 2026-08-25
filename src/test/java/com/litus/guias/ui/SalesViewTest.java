package com.litus.guias.ui;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalesViewTest {
    @Test
    void keepsOnlyPositiveQuantitiesForMultipleSale() {
        Map<Long, String> values = new LinkedHashMap<>();
        values.put(1L, "2");
        values.put(2L, "0");
        values.put(3L, "3");

        assertEquals(Map.of(1L, 2, 3L, 3), SalesView.parseMultipleQuantities(values));
    }

    @Test
    void rejectsEmptyOrInvalidMultipleSale() {
        assertThrows(IllegalArgumentException.class,
                () -> SalesView.parseMultipleQuantities(Map.of(1L, "0")));
        assertThrows(IllegalArgumentException.class,
                () -> SalesView.parseMultipleQuantities(Map.of(1L, "dos")));
    }
}
