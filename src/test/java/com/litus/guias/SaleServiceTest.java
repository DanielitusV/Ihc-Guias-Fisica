package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

        SaleResult result = saleService.registerSale(
                guide,
                cashAccount,
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 19, 30)
        );

        assertEquals(4, guide.getStock());
        assertEquals(
                new BigDecimal("125.00"),
                cashAccount.getBalance()
        );

        assertEquals(new BigDecimal("25.00"), result.getSale().getPrice());
        assertEquals(PaymentMethod.CASH, result.getSale().getPaymentMethod());
        assertEquals(SaleStatus.ACTIVE, result.getSale().getStatus());

        assertEquals(
                AccountMovementType.INCOME,
                result.getMovement().getType()
        );

        assertEquals(
                AccountMovementConcept.SALE,
                result.getMovement().getConcept()
        );

        assertEquals(
                new BigDecimal("25.00"),
                result.getMovement().getAmount()
        );

        assertEquals(
                1,
                result.getMovement().getAccountId()
        );
    }

    @Test
    public void failedSaleDoesNotChangeAccountBalance() {
        Guide guide = new Guide(
                1,
                "Física I",
                new BigDecimal("25.00"),
                0
        );

        Account cashAccount = new Account(
                1,
                "Efectivo",
                new BigDecimal("100.00")
        );

        SaleService saleService = new SaleService();

        assertThrows(
                IllegalStateException.class,
                () -> saleService.registerSale(
                        guide,
                        cashAccount,
                        PaymentMethod.CASH,
                        LocalDateTime.of(2026, 8, 14, 19, 30)
                )
        );

        assertEquals(
                new BigDecimal("100.00"),
                cashAccount.getBalance()
        );
    }
}
