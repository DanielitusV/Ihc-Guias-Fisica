package com.litus.guias.persistence;

import com.litus.guias.sale.AuthorizedDelivery;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AuthorizedDeliveryRepository {
    private final Database database;

    public AuthorizedDeliveryRepository(Database database) {
        this.database = database;
    }

    public long save(Connection connection, AuthorizedDelivery delivery) throws Exception {
        String sql = """
                INSERT INTO authorized_deliveries
                (guide_id, quantity, beneficiary, authorized_by, reason, created_at, academic_term_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, delivery.guideId());
            statement.setInt(2, delivery.quantity());
            statement.setString(3, delivery.beneficiary().trim());
            statement.setString(4, delivery.authorizedBy().trim());
            statement.setString(5, delivery.reason().trim());
            statement.setString(6, delivery.createdAt().toString());
            statement.setLong(7, AcademicTermRepository.requireActiveId(connection));
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new IllegalStateException("No se pudo generar el identificador de la entrega");
    }

    public AuthorizedDelivery findById(Connection connection, long id) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT id, guide_id, quantity, beneficiary, authorized_by, reason, created_at
                FROM authorized_deliveries
                WHERE id = ?
                """)) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
                if (!result.next()) return null;
                return new AuthorizedDelivery(
                        result.getLong("id"), result.getLong("guide_id"),
                        result.getInt("quantity"), result.getString("beneficiary"),
                        result.getString("authorized_by"), result.getString("reason"),
                        LocalDateTime.parse(result.getString("created_at")));
            }
        }
    }

    public List<AuthorizedDelivery> findAll() throws Exception {
        try (Connection connection = database.getConnection()) {
            Long termId = AcademicTermRepository.activeIdOrNull(connection);
            return termId == null ? List.of() : query(connection, termId, null, null);
        }
    }

    public List<AuthorizedDelivery> findBetween(LocalDateTime start, LocalDateTime end) throws Exception {
        try (Connection connection = database.getConnection()) {
            Long termId = AcademicTermRepository.activeIdOrNull(connection);
            return termId == null ? List.of() : query(connection, termId, start, end);
        }
    }

    public List<AuthorizedDelivery> findBetweenByTerm(long termId,
            LocalDateTime start, LocalDateTime end) throws Exception {
        try (Connection connection = database.getConnection()) {
            return query(connection, termId, start, end);
        }
    }

    private List<AuthorizedDelivery> query(Connection connection, long termId,
            LocalDateTime start, LocalDateTime end) throws Exception {
        String sql = """
                SELECT id, guide_id, quantity, beneficiary, authorized_by, reason, created_at
                FROM authorized_deliveries
                WHERE academic_term_id = ?
                """ + (start == null ? "" : " AND created_at >= ? AND created_at < ?")
                + " ORDER BY created_at ASC, id ASC";
        List<AuthorizedDelivery> values = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, termId);
            if (start != null) {
                statement.setString(2, start.toString());
                statement.setString(3, end.toString());
            }
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    values.add(new AuthorizedDelivery(
                            result.getLong("id"), result.getLong("guide_id"),
                            result.getInt("quantity"), result.getString("beneficiary"),
                            result.getString("authorized_by"), result.getString("reason"),
                            LocalDateTime.parse(result.getString("created_at"))));
                }
            }
        }
        return values;
    }
}
