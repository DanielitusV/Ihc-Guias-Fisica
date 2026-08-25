package com.litus.guias.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AcademicTermRepository {
    private final Database database;

    public AcademicTermRepository(Database database) { this.database = database; }

    public AcademicTerm findActive() throws Exception {
        try (Connection connection = database.getConnection()) { return findActive(connection); }
    }

    public AcademicTerm findActive(Connection connection) throws Exception {
        try (var statement = connection.prepareStatement("""
                SELECT id, code, status, opened_at, closed_at
                FROM academic_terms WHERE status = 'OPEN'
                """); var result = statement.executeQuery()) {
            return result.next() ? map(result) : null;
        }
    }

    public AcademicTerm findById(long id) throws Exception {
        try (Connection connection = database.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT id, code, status, opened_at, closed_at
                     FROM academic_terms WHERE id = ?
                     """)) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) { return result.next() ? map(result) : null; }
        }
    }

    public List<AcademicTerm> findAll() throws Exception {
        try (Connection connection = database.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT id, code, status, opened_at, closed_at
                     FROM academic_terms ORDER BY substr(code, 3, 4) DESC, substr(code, 1, 1) DESC
                     """); var result = statement.executeQuery()) {
            List<AcademicTerm> values = new ArrayList<>();
            while (result.next()) values.add(map(result));
            return values;
        }
    }

    AcademicTerm create(Connection connection, String code, LocalDateTime openedAt) throws Exception {
        try (var statement = connection.prepareStatement("""
                INSERT INTO academic_terms(code, status, opened_at) VALUES (?, 'OPEN', ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, code);
            statement.setString(2, openedAt.toString());
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new IllegalStateException("Could not create academic term");
                return new AcademicTerm(keys.getLong(1), code, AcademicTermStatus.OPEN, openedAt, null);
            }
        }
    }

    void close(Connection connection, long id, LocalDateTime closedAt) throws Exception {
        try (var statement = connection.prepareStatement("""
                UPDATE academic_terms SET status = 'CLOSED', closed_at = ?
                WHERE id = ? AND status = 'OPEN'
                """)) {
            statement.setString(1, closedAt.toString());
            statement.setLong(2, id);
            if (statement.executeUpdate() != 1) throw new IllegalStateException("La gestión ya está cerrada");
        }
    }

    public static long requireActiveId(Connection connection) throws Exception {
        Long id = activeIdOrNull(connection);
        if (id == null) throw new IllegalStateException("No existe una gestión activa");
        return id;
    }

    public static Long activeIdOrNull(Connection connection) throws Exception {
        try (var statement = connection.prepareStatement(
                "SELECT id FROM academic_terms WHERE status = 'OPEN'");
             var result = statement.executeQuery()) {
            return result.next() ? result.getLong(1) : null;
        }
    }

    private AcademicTerm map(ResultSet result) throws Exception {
        String closed = result.getString("closed_at");
        return new AcademicTerm(result.getLong("id"), result.getString("code"),
                AcademicTermStatus.valueOf(result.getString("status")),
                LocalDateTime.parse(result.getString("opened_at")),
                closed == null ? null : LocalDateTime.parse(closed));
    }
}
