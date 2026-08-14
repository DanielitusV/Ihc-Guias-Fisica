package com.litus.guias;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    public void sellingWithZeroStockIsNotAllowed() {
        Guide guide = new Guide(
                1,
                "Física I",
                new BigDecimal("25.00"),
                0
        );

        assertThrows(
                IllegalStateException.class,
                guide::sellOne
        );
    }
}
