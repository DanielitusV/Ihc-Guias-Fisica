package com.litus.guias.persistence;

import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.SaleStatus;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SaleRepository {
    private final Database database;

    public SaleRepository(Database database) { this.database = database; }

    public long save(Sale sale) throws Exception {
        try (Connection connection = database.getConnection()) { return save(connection, sale); }
    }

    public long save(Connection connection, Sale sale) throws Exception {
        String sql = """
                INSERT INTO sales
                (guide_id, account_id, price, payment_method, status,
                 cancellation_reason, cancelled_at, created_at, academic_term_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, sale.getGuideId());
            statement.setLong(2, sale.getAccountId());
            statement.setBigDecimal(3, sale.getPrice());
            statement.setString(4, sale.getPaymentMethod().name());
            statement.setString(5, sale.getStatus().name());
            statement.setString(6, sale.getCancellationReason());
            statement.setString(7, sale.getCancelledAt() == null ? null : sale.getCancelledAt().toString());
            statement.setString(8, sale.getCreatedAt().toString());
            statement.setLong(9, AcademicTermRepository.requireActiveId(connection));
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new IllegalStateException("Could not generate sale ID");
    }

    public Sale findById(long id) throws Exception {
        try (Connection connection = database.getConnection()) { return findById(connection, id); }
    }

    public Sale findById(Connection connection, long id) throws Exception {
        String sql = """
                SELECT id, guide_id, account_id, price, payment_method,
                       status, cancellation_reason, cancelled_at, created_at
                FROM sales
                WHERE id = ?
                  AND academic_term_id = (SELECT id FROM academic_terms WHERE status = 'OPEN')
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) { return result.next() ? mapSale(result) : null; }
        }
    }

    public void update(Sale sale) throws Exception {
        try (Connection connection = database.getConnection()) { update(connection, sale); }
    }

    public void update(Connection connection, Sale sale) throws Exception {
        String sql = """
                UPDATE sales
                SET status = ?, cancellation_reason = ?, cancelled_at = ?
                WHERE id = ?
                  AND academic_term_id = (SELECT id FROM academic_terms WHERE status = 'OPEN')
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, sale.getStatus().name());
            statement.setString(2, sale.getCancellationReason());
            statement.setString(3, sale.getCancelledAt() == null ? null : sale.getCancelledAt().toString());
            statement.setLong(4, sale.getId());
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("No se puede modificar una venta histórica");
            }
        }
    }

    public List<Sale> findAll() throws Exception {
        try (Connection connection = database.getConnection()) { return findAll(connection); }
    }

    public List<Sale> findAll(Connection connection) throws Exception {
        Long activeId = AcademicTermRepository.activeIdOrNull(connection);
        if (activeId == null) return List.of();
        return query(connection, activeId, LocalDateTime.of(1, 1, 1, 0, 0),
                LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999));
    }

    public List<Sale> findAllByTerm(long academicTermId) throws Exception {
        try (Connection connection = database.getConnection()) {
            return query(connection, academicTermId, LocalDateTime.of(1, 1, 1, 0, 0),
                    LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999));
        }
    }

    public List<Sale> findBetween(LocalDateTime startInclusive, LocalDateTime endExclusive)
            throws Exception {
        try (Connection connection = database.getConnection()) {
            return findBetween(connection, startInclusive, endExclusive);
        }
    }

    public List<Sale> findBetween(Connection connection, LocalDateTime startInclusive,
            LocalDateTime endExclusive) throws Exception {
        Long activeId = AcademicTermRepository.activeIdOrNull(connection);
        return activeId == null ? List.of() : query(connection, activeId, startInclusive, endExclusive);
    }

    public List<Sale> findBetweenByTerm(long academicTermId, LocalDateTime startInclusive,
            LocalDateTime endExclusive) throws Exception {
        try (Connection connection = database.getConnection()) {
            return query(connection, academicTermId, startInclusive, endExclusive);
        }
    }

    private List<Sale> query(Connection connection, long academicTermId,
            LocalDateTime startInclusive, LocalDateTime endExclusive) throws Exception {
        String sql = """
                SELECT id, guide_id, account_id, price, payment_method,
                       status, cancellation_reason, cancelled_at, created_at
                FROM sales
                WHERE academic_term_id = ? AND created_at >= ? AND created_at < ?
                ORDER BY created_at ASC, id ASC
                """;
        List<Sale> values = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, academicTermId);
            statement.setString(2, startInclusive.toString());
            statement.setString(3, endExclusive.toString());
            try (var result = statement.executeQuery()) {
                while (result.next()) values.add(mapSale(result));
            }
        }
        return values;
    }

    private Sale mapSale(java.sql.ResultSet result) throws Exception {
        Sale sale = new Sale(
                result.getLong("id"), result.getLong("guide_id"), result.getLong("account_id"),
                result.getBigDecimal("price"), PaymentMethod.valueOf(result.getString("payment_method")),
                LocalDateTime.parse(result.getString("created_at")), SaleStatus.ACTIVE
        );
        if (SaleStatus.valueOf(result.getString("status")) == SaleStatus.CANCELLED) {
            String cancelledAt = result.getString("cancelled_at");
            sale.cancel(result.getString("cancellation_reason"),
                    cancelledAt == null ? null : LocalDateTime.parse(cancelledAt));
        }
        return sale;
    }
}
