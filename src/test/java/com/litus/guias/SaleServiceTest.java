package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

public class SaleServiceTest {

    @Test
    public void cashSaleReducesStockAndIncreasesAccountBalance() {
        Guide guide = new Guide(
                1,
                "Física I",
                new BigDecimal("25.00"),
                5
        );

        Account cashAccount = new Account(
                1,
                "Efectivo",
                new BigDecimal("100.00")
        );

        SaleService saleService = new SaleService();

        saleService.registerSale(guide, cashAccount);

        assertEquals(4, guide.getStock());
        assertEquals(
                new BigDecimal("125.00"),
                cashAccount.getBalance()
        );
    }
}
