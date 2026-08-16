package com.litus.guias.persistence;

import com.litus.guias.closure.CashClosure;
import com.litus.guias.closure.DayStatus;
import com.litus.guias.closure.DayStatusCalculator;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DayStatusQueryService {

    private final Database database;
    private final CashClosureRepository closureRepository;
    private final DayStatusCalculator calculator;

    public DayStatusQueryService(Database database) {
        this.database = database;
        this.closureRepository = new CashClosureRepository(database);
        this.calculator = new DayStatusCalculator();
    }

    public DayStatus getStatus(LocalDate day, LocalDate today) throws Exception {
        try (Connection connection = database.getConnection()) {
            List<CashClosure> closures =
                    closureRepository.findAllByDate(connection, day);
            return calculator.calculate(
                    day,
                    hasActivity(connection, day),
                    closures,
                    today
            );
        }
    }

    private boolean hasActivity(Connection connection, LocalDate day)
            throws Exception {
        String sql = """
                SELECT EXISTS (
                    SELECT 1 FROM sales
                    WHERE created_at >= ? AND created_at < ?
                    UNION ALL
                    SELECT 1 FROM orders
                    WHERE created_at >= ? AND created_at < ?
                    UNION ALL
                    SELECT 1 FROM account_movements
                    WHERE created_at >= ? AND created_at < ?
                    UNION ALL
                    SELECT 1 FROM cash_closures
                    WHERE created_at >= ? AND created_at < ?
                )
                """;
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();
        try (var statement = connection.prepareStatement(sql)) {
            for (int pair = 0; pair < 4; pair++) {
                statement.setString(pair * 2 + 1, start.toString());
                statement.setString(pair * 2 + 2, end.toString());
            }
            try (var result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }
}
