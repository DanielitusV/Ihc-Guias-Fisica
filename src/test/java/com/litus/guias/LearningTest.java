package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LearningTest {

    @Test
    public void twoPlusTwoEqualsFour() {
        int result = 2 + 2;

        assertEquals(4, result);
    }

    @Test
    public void invalidNumberThrowsException() {
        assertThrows(
                NumberFormatException.class,
                () -> Integer.parseInt("Papulince")
        );
    }
}