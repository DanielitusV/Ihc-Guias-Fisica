package com.litus.guias.persistence;

import com.litus.guias.account.CashClosureItem;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.closure.CashClosureStatus;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CashClosureRepository {

    private final Database database;

    public CashClosureRepository(Database database) {
        this.database = database;
    }

    public long save(
            CashClosure closure,
            List<CashClosureItem> items
    ) throws Exception {

        return database.inTransaction(
                connection -> save(connection, closure, items)
        );
    }

    public long save(
            Connection connection,
            CashClosure closure,
            List<CashClosureItem> items
    ) throws Exception {

        String closureSql = """
                INSERT INTO cash_closures
                (
                    expected_cash,
                    counted_cash,
                    expected_qr,
                    reported_qr,
                    notes,
                    status,
                    cancellation_reason,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        long closureId;

        try (var statement = connection.prepareStatement(
                closureSql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setBigDecimal(1, closure.getExpectedCash());
            statement.setBigDecimal(2, closure.getCountedCash());
            statement.setBigDecimal(3, closure.getExpectedQr());
            statement.setBigDecimal(4, closure.getReportedQr());
            statement.setString(5, closure.getNotes());
            statement.setString(6, closure.getStatus().name());
            statement.setString(7, closure.getCancellationReason());
            statement.setString(8, closure.getCreatedAt().toString());

            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new IllegalStateException(
                            "Could not generate cash closure ID"
                    );
                }

                closureId = keys.getLong(1);
            }
        }

        String itemSql = """
                INSERT INTO cash_closure_items
                (
                    cash_closure_id,
                    guide_id,
                    expected_stock,
                    counted_stock
                )
                VALUES (?, ?, ?, ?)
                """;

        for (CashClosureItem item : items) {
            try (var statement =
                         connection.prepareStatement(itemSql)) {

                statement.setLong(1, closureId);
                statement.setLong(2, item.getGuideId());
                statement.setInt(3, item.getExpectedStock());
                statement.setInt(4, item.getCountedStock());

                statement.executeUpdate();
            }
        }

        return closureId;
    }

    public CashClosure findById(long id) throws Exception {
        try (Connection connection = database.getConnection()) {
            return findById(connection, id);
        }
    }

    public CashClosure findById(
            Connection connection,
            long id
    ) throws Exception {

        String sql = """
                SELECT
                    id,
                    expected_cash,
                    counted_cash,
                    expected_qr,
                    reported_qr,
                    notes,
                    status,
                    cancellation_reason,
                    created_at
                FROM cash_closures
                WHERE id = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (var result = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                return mapClosure(result);
            }
        }
    }

    public CashClosure findValidByDate(
            LocalDate date
    ) throws Exception {

        try (Connection connection = database.getConnection()) {
            return findValidByDate(connection, date);
        }
    }

    public CashClosure findValidByDate(
            Connection connection,
            LocalDate date
    ) throws Exception {

        String sql = """
                SELECT
                    id,
                    expected_cash,
                    counted_cash,
                    expected_qr,
                    reported_qr,
                    notes,
                    status,
                    cancellation_reason,
                    created_at
                FROM cash_closures
                WHERE status = 'VALID'
                  AND created_at >= ?
                  AND created_at < ?
                ORDER BY created_at DESC
                LIMIT 1
                """;

        LocalDateTime start =
                date.atStartOfDay();

        LocalDateTime end =
                date.plusDays(1).atStartOfDay();

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, start.toString());
            statement.setString(2, end.toString());

            try (var result = statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                return mapClosure(result);
            }
        }
    }

    public List<CashClosureItem> findItemsByClosureId(
            long closureId
    ) throws Exception {

        try (Connection connection = database.getConnection()) {
            return findItemsByClosureId(connection, closureId);
        }
    }

    public List<CashClosureItem> findItemsByClosureId(
            Connection connection,
            long closureId
    ) throws Exception {

        String sql = """
                SELECT
                    id,
                    cash_closure_id,
                    guide_id,
                    expected_stock,
                    counted_stock
                FROM cash_closure_items
                WHERE cash_closure_id = ?
                """;

        List<CashClosureItem> items =
                new ArrayList<>();

        try (var statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, closureId);

            try (var result = statement.executeQuery()) {

                while (result.next()) {
                    items.add(
                            new CashClosureItem(
                                    result.getLong("id"),
                                    result.getLong("cash_closure_id"),
                                    result.getLong("guide_id"),
                                    result.getInt("expected_stock"),
                                    result.getInt("counted_stock")
                            )
                    );
                }
            }
        }

        return items;
    }

    public void update(
            CashClosure closure
    ) throws Exception {

        try (Connection connection = database.getConnection()) {
            update(connection, closure);
        }
    }

    public List<CashClosure> findAll() throws Exception {
        try (Connection connection = database.getConnection()) {
            return findAll(connection);
        }
    }

    public List<CashClosure> findAll(Connection connection) throws Exception {
        String sql = """
                SELECT id, expected_cash, counted_cash, expected_qr,
                       reported_qr, notes, status, cancellation_reason,
                       created_at
                FROM cash_closures
                ORDER BY created_at ASC, id ASC
                """;
        return queryClosures(connection, sql, null, null);
    }

    public List<CashClosure> findAllByDate(LocalDate date) throws Exception {
        try (Connection connection = database.getConnection()) {
            return findAllByDate(connection, date);
        }
    }

    public List<CashClosure> findAllByDate(
            Connection connection,
            LocalDate date
    ) throws Exception {
        String sql = """
                SELECT id, expected_cash, counted_cash, expected_qr,
                       reported_qr, notes, status, cancellation_reason,
                       created_at
                FROM cash_closures
                WHERE created_at >= ? AND created_at < ?
                ORDER BY created_at ASC, id ASC
                """;
        return queryClosures(
                connection,
                sql,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );
    }

    private List<CashClosure> queryClosures(
            Connection connection,
            String sql,
            LocalDateTime start,
            LocalDateTime end
    ) throws Exception {
        List<CashClosure> closures = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            if (start != null) {
                statement.setString(1, start.toString());
                statement.setString(2, end.toString());
            }
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    closures.add(mapClosure(result));
                }
            }
        }
        return closures;
    }

    public void update(
            Connection connection,
            CashClosure closure
    ) throws Exception {

        String sql = """
                UPDATE cash_closures
                SET status = ?,
                    cancellation_reason = ?
                WHERE id = ?
                """;

        try (var statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    closure.getStatus().name()
            );

            statement.setString(
                    2,
                    closure.getCancellationReason()
            );

            statement.setLong(
                    3,
                    closure.getId()
            );

            statement.executeUpdate();
        }
    }

    private CashClosure mapClosure(
            java.sql.ResultSet result
    ) throws Exception {

        CashClosure closure = new CashClosure(
                result.getLong("id"),
                result.getBigDecimal("expected_cash"),
                result.getBigDecimal("counted_cash"),
                result.getBigDecimal("expected_qr"),
                result.getBigDecimal("reported_qr"),
                result.getString("notes"),
                LocalDateTime.parse(
                        result.getString("created_at")
                )
        );

        CashClosureStatus status =
                CashClosureStatus.valueOf(
                        result.getString("status")
                );

        if (status == CashClosureStatus.CANCELLED) {
            closure.cancel(
                    result.getString("cancellation_reason")
            );
        }

        return closure;
    }
}
