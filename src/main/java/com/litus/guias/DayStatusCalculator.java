package com.litus.guias;

import java.time.LocalDate;
import java.util.List;

public class DayStatusCalculator {

    public DayStatus calculate(
            LocalDate day,
            boolean hasActivity,
            List<CashClosure> closures,
            LocalDate today
    ) {
        for (CashClosure closure : closures) {
            boolean sameDay =
                    closure.getCreatedAt()
                            .toLocalDate()
                            .equals(day);

            boolean isValid =
                    closure.getStatus()
                    == CashClosureStatus.VALID;

            if (sameDay && isValid) {
                return DayStatus.CLOSED;
            }
        }

        if (day.isBefore(today) && hasActivity) {
            return DayStatus.MISSED;
        }

        if (day.isBefore(today) && !hasActivity) {
            return DayStatus.NO_ACTIVITY;
        }

        return DayStatus.OPEN;
    }
}
