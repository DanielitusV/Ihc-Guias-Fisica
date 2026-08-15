package com.litus.guias;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CashClosureServiceTest {

    @Test
    public void cannotRegisterTwoValidClosuresOnSameDay() {
        CashClosure firstClosure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                "Primer cierre",
                LocalDateTime.of(2026, 8, 15, 15, 0)
        );

        CashClosure secondClosure = new CashClosure(
                2,
                new BigDecimal("600.00"),
                new BigDecimal("600.00"),
                new BigDecimal("350.00"),
                new BigDecimal("350.00"),
                "Segundo cierre",
                LocalDateTime.of(2026, 8, 15, 20, 0)
        );

        CashClosureService service = new CashClosureService();

        assertThrows(
                IllegalStateException.class,
                () -> service.registerClosure(
                        secondClosure,
                        List.of(firstClosure)
                )
        );
    }

    @Test
    public void cancelledClosureAllowsNewValidClosureOnSameDay() {
        CashClosure cancelledClosure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                "Cierre prematuro",
                LocalDateTime.of(2026, 8, 15, 15, 0)
        );

        cancelledClosure.cancel(
                "Se continuó atendiendo"
        );

        CashClosure newClosure = new CashClosure(
                2,
                new BigDecimal("700.00"),
                new BigDecimal("700.00"),
                new BigDecimal("400.00"),
                new BigDecimal("400.00"),
                "Cierre final",
                LocalDateTime.of(2026,8, 15, 20, 0)
        );

        CashClosureService service = new CashClosureService();

        assertDoesNotThrow(
                () -> service.registerClosure(
                        newClosure,
                        List.of(cancelledClosure)
                )
        );
    }
}
