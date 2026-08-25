package com.litus.guias.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;

public final class AcademicTermService {
    private static final byte[] PASSWORD_HASH = HexFormat.of().parseHex(
            "d83a3734870d467772a5102cf11570c6b1f9a140888fe8ecf877245a37338cad");
    private final Database database;
    private final AcademicTermRepository repository;

    public AcademicTermService(Database database) {
        this.database = database;
        this.repository = new AcademicTermRepository(database);
    }

    public AcademicTerm open(String code, LocalDateTime openedAt) throws Exception {
        String normalized = code == null ? "" : code.trim();
        if (!normalized.matches("[12]-\\d{4}")) {
            throw new IllegalArgumentException("La gestión debe tener formato 1-AAAA o 2-AAAA");
        }
        return database.inTransaction(connection -> {
            if (repository.findActive(connection) != null) {
                throw new IllegalStateException("Ya existe una gestión activa");
            }
            return repository.create(connection, normalized, openedAt);
        });
    }

    public void closeActive(String password, boolean confirmed, LocalDateTime closedAt) throws Exception {
        if (!verifyPassword(password)) throw new SecurityException("Contraseña incorrecta");
        if (!confirmed) throw new IllegalArgumentException("El cierre requiere segunda confirmación");
        database.inTransaction(connection -> {
            AcademicTerm active = repository.findActive(connection);
            if (active == null) throw new IllegalStateException("No existe una gestión activa");
            try (var statement = connection.prepareStatement("""
                    SELECT EXISTS (
                        SELECT 1
                        FROM orders o
                        JOIN order_items i ON i.order_id = o.id
                        WHERE o.academic_term_id = ?
                          AND o.status = 'ACTIVE'
                          AND i.unit_cost = 0
                    )
                    """)) {
                statement.setLong(1, active.id());
                try (var result = statement.executeQuery()) {
                    if (result.next() && result.getBoolean(1)) {
                        throw new IllegalStateException(
                                "Existen pedidos con costo pendiente; complétalos antes de cerrar la gestión");
                    }
                }
            }
            repository.close(connection, active.id(), closedAt);
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE guides SET stock = 0");
                statement.executeUpdate("UPDATE accounts SET balance = 0");
            }
            return null;
        });
    }

    public static String defaultCode(LocalDate date) {
        return (date.getMonthValue() <= 6 ? "1-" : "2-") + date.getYear();
    }

    public boolean verifyPassword(String password) throws Exception {
        return passwordMatches(password);
    }

    private boolean passwordMatches(String password) throws Exception {
        byte[] supplied = MessageDigest.getInstance("SHA-256").digest(
                (password == null ? "" : password).getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(PASSWORD_HASH, supplied);
    }
}
