package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

public class GuideTest {

    @Test
    public void negativeStockIsNotAllowed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Guide(
                        1,
                        "Física I",
                        new BigDecimal("25.00"),
                        -1
                )
        );
    }
}
