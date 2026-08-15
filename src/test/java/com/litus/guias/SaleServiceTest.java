package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    @Test
    void cancellingSaleRestoresStockAndMoney() {
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

        SaleService service = new SaleService();

        SaleResult saleResult = service.registerSale(
                guide,
                cashAccount,
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 19, 30)
        );

        AccountMovement cancellationMovement = service.cancelSale(
                saleResult.getSale(),
                guide,
                cashAccount,
                "Venta duplicada",
                LocalDateTime.of(2026, 8, 14, 19, 30)
        );

        assertEquals(SaleStatus.CANCELLED, saleResult.getSale().getStatus());
        assertEquals(5, guide.getStock());
        assertEquals(new BigDecimal("100.00"), cashAccount.getBalance());

        assertEquals(
                AccountMovementType.EXPENSE,
                cancellationMovement.getType()
        );

        assertEquals(
                AccountMovementConcept.SALE_CANCELLATION,
                cancellationMovement.getConcept()
        );

        assertEquals(
                new BigDecimal("25.00"),
                cancellationMovement.getAmount()
        );
    }

    @Test
    public void saleCannotBeCancelledTwice() {
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

        SaleService service = new SaleService();

        SaleResult result = service.registerSale(
                guide,
                cashAccount,
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 19, 30)
        );

        service.cancelSale(
                result.getSale(),
                guide,
                cashAccount,
                "Venta duplicada",
                LocalDateTime.of(2026, 8, 14, 19, 35)
        );

        assertThrows(
                IllegalStateException.class,
                () -> service.cancelSale(
                        result.getSale(),
                        guide,
                        cashAccount,
                        "Otra vez",
                        LocalDateTime.of(2026, 8, 14, 19, 40)
                )
        );

        assertEquals(5, guide.getStock());
        assertEquals(new BigDecimal("100.00"), cashAccount.getBalance());
    }

    @Test
    public void saleCannotBeRegisteredAfterValidClosureOnSameDay() {
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

        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Cierre del día",
                LocalDateTime.of(2026, 8, 15, 15, 30)
        );

        SaleService service = new SaleService();

        assertThrows(
                IllegalStateException.class,
                () -> service.registerSale(
                        guide,
                        cashAccount,
                        PaymentMethod.CASH,
                        LocalDateTime.of(2026, 8, 15, 17, 0),
                        List.of(closure)
                )
        );

        assertEquals(5, guide.getStock());
        assertEquals(new BigDecimal("100.00"), cashAccount.getBalance());
    }

    @Test
    public void saleCanBeRegisteredAfterClosureIsCancelled() {
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

        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Cierre prematuro",
                LocalDateTime.of(2026, 8, 15, 15, 0)
        );

        closure.cancel(
                "Se continuó atendiendo después del cierre"
        );

        SaleService service = new SaleService();

        SaleResult result = service.registerSale(
               guide,
               cashAccount,
               PaymentMethod.CASH,
               LocalDateTime.of(2026, 8, 15, 17, 0),
               List.of(closure)
        );

        assertEquals(4, guide.getStock());

        assertEquals(
                new BigDecimal("125.00"),
                cashAccount.getBalance()
        );

        assertEquals(
                SaleStatus.ACTIVE,
                result.getSale().getStatus()
        );

        assertEquals(
                new BigDecimal("25.00"),
                result.getMovement().getAmount()
        );
    }
}
