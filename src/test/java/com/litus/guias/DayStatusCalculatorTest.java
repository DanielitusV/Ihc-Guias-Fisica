package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DayStatusCalculatorTest {

    @Test
    public void pastDayWithActivityAndNoValidClosureIsMissed() {
        DayStatusCalculator calculator = new DayStatusCalculator();

        DayStatus status = calculator.calculate(
                LocalDate.of(2026, 8, 14),
                true,
                List.of(),
                LocalDate.of(2026,8 , 15)
        );

        assertEquals(
                DayStatus.MISSED,
                status
        );
    }

    @Test
    public void pastDayWithValidClosureIsClosed() {
        DayStatusCalculator calculator = new DayStatusCalculator();

        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                "Cierre correcto",
                LocalDateTime.of(2026, 8, 14, 20, 0)
        );

        DayStatus status = calculator.calculate(
                LocalDate.of(2026, 8, 14),
                true,
                List.of(closure),
                LocalDate.of(2026, 8, 15)
        );

        assertEquals(
                DayStatus.CLOSED,
                status
        );
    }

    @Test
    public void pastDayWithOnlyCancelledClosureIsMissed() {
        DayStatusCalculator calculator = new DayStatusCalculator();

        CashClosure closure = new CashClosure(
                1,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                new BigDecimal("300.00"),
                new BigDecimal("300.00"),
                "Cierre prematuro",
                LocalDateTime.of(2026, 8, 14, 15, 0)
        );

        closure.cancel(
                "Se continuó atendiendo después del cierre"
        );

        DayStatus status = calculator.calculate(
                LocalDate.of(2026, 8, 14),
                true,
                List.of(closure),
                LocalDate.of(2026, 8, 15)
        );

        assertEquals(
                DayStatus.MISSED,
                status
        );
    }
}
