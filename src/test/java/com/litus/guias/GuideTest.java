package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    public void sellingOneGuideReducesStockByOne() {
        Guide guide = new Guide(
                1,
                "Física I",
                new BigDecimal("25.00"),
                5
        );

        guide.sellOne();

        assertEquals(4, guide.getStock());
    }
}
