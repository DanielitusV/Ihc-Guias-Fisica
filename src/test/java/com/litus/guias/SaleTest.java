package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

public class SaleTest {

    @Test
    public void saleKeepsThePriceUsedAtTheMomentOfSale() {
        Sale sale = new Sale(
                1,
                10,
                new BigDecimal("25.00")
        );

        assertEquals(
                new BigDecimal("25.00"),
                sale.getPrice()
        );
    }
}
