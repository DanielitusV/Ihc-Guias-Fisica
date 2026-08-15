package com.litus.guias.closure;

import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.SaleStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CashClosureCalculatorTest {

    @Test
    public void calculatesSoldQuantityFromInventory() {
        CashClosureCalculator calculator = new CashClosureCalculator();

        int soldQuantity = calculator.calculateSoldQuantity(
                50, // stock cierre anterior,
                10, // guías recibidas
                37  // guías contadas ahora
        );
        assertEquals(23, soldQuantity);
    }

    @Test
    public void calculatesSalesDifferenceAgainstRegisteredSales() {
        CashClosureCalculator calculator = new CashClosureCalculator();

        int difference = calculator.calculateSalesDifference(
                23, // vendidas según inventario
                20  // ventas registradas
        );

        assertEquals(-3, difference);
    }

    @Test
    public void calculatesExpectedMoneyFromSoldQuantity() {
        CashClosureCalculator calculator = new CashClosureCalculator();

        BigDecimal expectedMoney = calculator.calculatedExpectedMoney(
                23,
                new BigDecimal("25.00")
        );

        assertEquals(
                new BigDecimal("575.00"),
                expectedMoney
        );
    }

    @Test
    public void calculatesTotalExpectedMoneyForAllGuides() {
        CashClosureCalculator calculator = new CashClosureCalculator();

        BigDecimal fisicaI = calculator.calculatedExpectedMoney(
                10,
                new BigDecimal("25.00")
        );

        BigDecimal fisicaII = calculator.calculatedExpectedMoney(
                5,
                new BigDecimal("30.00")
        );

        BigDecimal total = calculator.calculatedTotalExpectedMoney(
                List.of(fisicaI, fisicaII)
        );

        assertEquals(
                new BigDecimal("400.00"),
                total
        );
    }

    @Test
    public void calculatesMoneyDifferenceAgainstRegisteredSales() {
        CashClosureCalculator calculator = new CashClosureCalculator();

        BigDecimal difference = calculator.calculateMoneyDifference(
                new BigDecimal("575.00"), //esperado por inventario
                new BigDecimal("525.00")
        );

        assertEquals(
                new BigDecimal("-50.00"),
                difference
        );
    }

    @Test
    public void calculatesRegisteredSalesTotalByPaymentMethod() {
        CashClosureCalculator calculator = new CashClosureCalculator();

        Sale cashSale1 = new Sale(
                1,
                1,
                new BigDecimal("25.00"),
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 10, 0),
                SaleStatus.ACTIVE

        );

        Sale qrSale = new Sale(
                2,
                2,
                new BigDecimal("30.00"),
                PaymentMethod.QR,
                LocalDateTime.of(2026, 8, 14, 10, 0),
                SaleStatus.ACTIVE
        );

        Sale cashSale2 = new Sale(
                3,
                1,
                new BigDecimal("25.00"),
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 10, 0),
                SaleStatus.ACTIVE
        );

        BigDecimal cashTotal =
                calculator.calculateRegisteredSalesTotal(
                        List.of(cashSale1, qrSale, cashSale2),
                        PaymentMethod.CASH
                );

        assertEquals(
                new BigDecimal("50.00"),
                cashTotal
        );
    }

    @Test
    public void cancelledSalesAreNotIncludedRegisteredSalesTotal() {
        CashClosureCalculator calculator = new CashClosureCalculator();

        Sale activeSale = new Sale(
                1,
                1,
                new BigDecimal("25.00"),
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 10, 0),
                SaleStatus.ACTIVE
        );

        Sale cancelledSale = new Sale(
                2,
                1,
                new BigDecimal("25.00"),
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 11, 0),
                SaleStatus.ACTIVE
        );

        cancelledSale.cancel("Venta registrada por error");

        BigDecimal total =
                calculator.calculateRegisteredSalesTotal(
                        List.of(activeSale, cancelledSale),
                        PaymentMethod.CASH
                );

        assertEquals(
                new BigDecimal("25.00"),
                total
        );
    }

    @Test
    public void registeredSalesTotalOnlyIncludesSalesFromClosureDay() {
        CashClosureCalculator calculator = new CashClosureCalculator();

        LocalDateTime closureTime = LocalDateTime.of(2026, 8, 15, 20, 0);

        Sale yesterdaySale = new Sale(
                1,
                1,
                new BigDecimal("25.00"),
                PaymentMethod.CASH,
                LocalDateTime.of(2026,8,14,18, 0),
                SaleStatus.ACTIVE
        );

        Sale todaySale = new Sale(
                2,
                1,
                new BigDecimal("25.00"),
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 15, 10, 0),
                SaleStatus.ACTIVE
        );

        Sale afterClosure = new Sale(
                3,
                1,
                new BigDecimal("25.00"),
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 15, 21, 0),
                SaleStatus.ACTIVE
        );

        BigDecimal total =
                calculator.calculateRegisteredSalesTotal(
                        List.of(yesterdaySale, todaySale, afterClosure),
                        PaymentMethod.CASH,
                        closureTime
                );

        assertEquals(new BigDecimal("25.00"), total);
    }
}
