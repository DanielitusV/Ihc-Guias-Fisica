package com.litus.guias.persistence;

import com.litus.guias.inventory.InventoryAdjustment;
import com.litus.guias.inventory.InventoryAdjustmentType;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class InventoryAdjustmentRepository {
    private final Database database;

    public InventoryAdjustmentRepository(Database database) {
        this.database = database;
    }

    public long save(Connection connection, InventoryAdjustment adjustment) throws Exception {
        String sql = """
                INSERT INTO inventory_adjustments
                (guide_id, quantity_delta, adjustment_type, reason, created_at, cash_closure_id, academic_term_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, adjustment.guideId());
            statement.setInt(2, adjustment.quantityDelta());
            statement.setString(3, adjustment.type().name());
            statement.setString(4, adjustment.reason());
            statement.setString(5, adjustment.createdAt().toString());
            if (adjustment.cashClosureId() == null) statement.setNull(6, java.sql.Types.INTEGER);
            else statement.setLong(6, adjustment.cashClosureId());
            statement.setLong(7, AcademicTermRepository.requireActiveId(connection));
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new IllegalStateException("Could not generate inventory adjustment ID");
    }

    public List<InventoryAdjustment> findAll() throws Exception {
        try (Connection connection = database.getConnection()) {
            Long activeId = AcademicTermRepository.activeIdOrNull(connection);
            return activeId == null ? List.of() : findAllByTerm(connection, activeId);
        }
    }

    public List<InventoryAdjustment> findAllByTerm(long academicTermId) throws Exception {
        try (Connection connection = database.getConnection()) {
            return findAllByTerm(connection, academicTermId);
        }
    }

    private List<InventoryAdjustment> findAllByTerm(Connection connection, long academicTermId)
            throws Exception {
        String sql = """
                SELECT id, guide_id, quantity_delta, adjustment_type, reason, created_at, cash_closure_id
                FROM inventory_adjustments
                WHERE academic_term_id = ?
                ORDER BY created_at ASC, id ASC
                """;
        List<InventoryAdjustment> values = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, academicTermId);
            try (var result = statement.executeQuery()) {
                while (result.next()) values.add(map(result));
            }
        }
        return values;
    }

    public List<InventoryAdjustment> findBetween(LocalDateTime startInclusive,
            LocalDateTime endExclusive) throws Exception {
        try (Connection connection = database.getConnection()) {
            Long activeId = AcademicTermRepository.activeIdOrNull(connection);
            if (activeId == null) return List.of();
            String sql = """
                    SELECT id, guide_id, quantity_delta, adjustment_type, reason, created_at, cash_closure_id
                    FROM inventory_adjustments
                    WHERE academic_term_id = ? AND created_at >= ? AND created_at < ?
                    ORDER BY created_at ASC, id ASC
                    """;
            List<InventoryAdjustment> values = new ArrayList<>();
            try (var statement = connection.prepareStatement(sql)) {
                statement.setLong(1, activeId);
                statement.setString(2, startInclusive.toString());
                statement.setString(3, endExclusive.toString());
                try (var result = statement.executeQuery()) {
                    while (result.next()) values.add(map(result));
                }
            }
            return values;
        }
    }

    private InventoryAdjustment map(java.sql.ResultSet result) throws Exception {
        long closureId = result.getLong("cash_closure_id");
        boolean closureWasNull = result.wasNull();
        return new InventoryAdjustment(
                result.getLong("id"),
                result.getLong("guide_id"),
                result.getInt("quantity_delta"),
                InventoryAdjustmentType.valueOf(result.getString("adjustment_type")),
                result.getString("reason"),
                LocalDateTime.parse(result.getString("created_at")),
                closureWasNull ? null : closureId
        );
    }

    public List<InventoryAdjustment> findByClosureId(Connection connection, long closureId)
            throws Exception {
        String sql = """
                SELECT id, guide_id, quantity_delta, adjustment_type, reason, created_at, cash_closure_id
                FROM inventory_adjustments
                WHERE cash_closure_id = ?
                ORDER BY id ASC
                """;
        List<InventoryAdjustment> values = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, closureId);
            try (var result = statement.executeQuery()) {
                while (result.next()) values.add(map(result));
            }
        }
        return values;
    }
}
