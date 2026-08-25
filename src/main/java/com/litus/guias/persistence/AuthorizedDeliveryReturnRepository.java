package com.litus.guias.persistence;

import com.litus.guias.sale.AuthorizedDeliveryReturn;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AuthorizedDeliveryReturnRepository {
    private final Database database;

    public AuthorizedDeliveryReturnRepository(Database database) {
        this.database = database;
    }

    public long save(Connection connection, AuthorizedDeliveryReturn value) throws Exception {
        String sql = """
                INSERT INTO authorized_delivery_returns
                (delivery_id, quantity, reason, created_at, academic_term_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, value.deliveryId());
            statement.setInt(2, value.quantity());
            statement.setString(3, value.reason().trim());
            statement.setString(4, value.createdAt().toString());
            statement.setLong(5, AcademicTermRepository.requireActiveId(connection));
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new IllegalStateException("No se pudo generar el identificador de la devolución");
    }

    public int returnedQuantity(Connection connection, long deliveryId) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT COALESCE(SUM(quantity), 0)
                FROM authorized_delivery_returns
                WHERE delivery_id = ?
                """)) {
            statement.setLong(1, deliveryId);
            try (var result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    public List<AuthorizedDeliveryReturn> findAll() throws Exception {
        try (Connection connection = database.getConnection()) {
            Long termId = AcademicTermRepository.activeIdOrNull(connection);
            return termId == null ? List.of() : query(connection, termId, null, null);
        }
    }

    public List<AuthorizedDeliveryReturn> findBetween(LocalDateTime start, LocalDateTime end) throws Exception {
        try (Connection connection = database.getConnection()) {
            Long termId = AcademicTermRepository.activeIdOrNull(connection);
            return termId == null ? List.of() : query(connection, termId, start, end);
        }
    }

    public List<AuthorizedDeliveryReturn> findBetweenByTerm(
            long termId, LocalDateTime start, LocalDateTime end) throws Exception {
        try (Connection connection = database.getConnection()) {
            return query(connection, termId, start, end);
        }
    }

    private List<AuthorizedDeliveryReturn> query(
            Connection connection, long termId, LocalDateTime start, LocalDateTime end) throws Exception {
        String sql = """
                SELECT r.id, r.delivery_id, d.guide_id, r.quantity, r.reason, r.created_at
                FROM authorized_delivery_returns r
                JOIN authorized_deliveries d ON d.id = r.delivery_id
                WHERE r.academic_term_id = ?
                """ + (start == null ? "" : " AND r.created_at >= ? AND r.created_at < ?")
                + " ORDER BY r.created_at ASC, r.id ASC";
        List<AuthorizedDeliveryReturn> values = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, termId);
            if (start != null) {
                statement.setString(2, start.toString());
                statement.setString(3, end.toString());
            }
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    values.add(new AuthorizedDeliveryReturn(
                            result.getLong("id"), result.getLong("delivery_id"),
                            result.getLong("guide_id"), result.getInt("quantity"),
                            result.getString("reason"),
                            LocalDateTime.parse(result.getString("created_at"))));
                }
            }
        }
        return values;
    }
}
