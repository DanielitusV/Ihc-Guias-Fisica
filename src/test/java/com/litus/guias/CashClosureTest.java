package com.litus.guias;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CashClosureTest {

    @Test
    public void calculateCashDifference() {
        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("480.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                "Cierre de prueba",
                LocalDateTime.of(2026, 9, 14, 21, 30)
        );

        assertEquals(new BigDecimal("-20.00"), closure.getCashDifference());
    }

    @Test
    public void calculatesQrDifference() {
        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("280.00"),
                "Cierre de prueba",
                LocalDateTime.of(2026, 9, 14, 21, 30)
        );

        assertEquals(new BigDecimal("-20.00"), closure.getQrDifference());
    }

    @Test
    public void closureIsBalanceWhenCashQrAndStockMatch() {
        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                "Cierre correcto",
                LocalDateTime.of(2026, 9, 14, 21, 30)
        );

        CashClosureItem fisicaI = new CashClosureItem(
                1,
                1,
                1,
                50,
                50
        );

        CashClosureItem fisicaII = new CashClosureItem(
                2,
                1,
                2,
                30,
                30
        );

        assertTrue(
                closure.isBalanced(
                        List.of(fisicaI, fisicaII)
                )
        );
    }

    @Test
    public void newClosureStartsAsValid() {
        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                "Cierre normal",
                LocalDateTime.of(2026, 8, 15, 20, 0)
        );

        assertEquals(
                CashClosureStatus.VALID,
                closure.getStatus()
        );
    }

    @Test
    public void cancelingClosureChangesStatusAndStoresReason() {
        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                "Cierre prematuro",
                LocalDateTime.of(2026, 8, 15, 15, 0)
        );

        closure.cancel(
                "Se continuó atendiendo después del cierre"
        );

        assertEquals(
                CashClosureStatus.CANCELLED,
                closure.getStatus()
        );

        assertEquals(
                "Se continuó atendiendo después del cierre",
                closure.getCancellationReason()
        );
    }

    @Test
    public void closureCannotBeCancelledTwice() {
        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                "Cierre prematuro",
                LocalDateTime.of(2026, 8, 15, 15, 0)
        );

        closure.cancel("Se continuó atendiendo");

        assertThrows(
                IllegalStateException.class,
                () -> closure.cancel("Otro motivo")
        );

        assertEquals(
                "Se continuó atendiendo",
                closure.getCancellationReason()
        );
    }
}
