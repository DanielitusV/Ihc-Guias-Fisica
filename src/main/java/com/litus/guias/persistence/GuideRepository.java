package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;

import java.sql.Connection;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuideRepository {

    private final Database database;

    public GuideRepository(Database database) {
        this.database = database;
    }

    public long save(Guide guide) throws Exception {
        try (Connection connection = database.getConnection()) {
            return save(connection, guide);
        }
    }

    public long save(Connection connection, Guide guide) throws Exception {
        String sql = """
                INSERT INTO guides (name, current_price, default_unit_cost, stock)
                VALUES (?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setString(1, guide.getName());
            statement.setBigDecimal(2, guide.getCurrentPrice());
            statement.setBigDecimal(3, guide.getDefaultUnitCost());
            statement.setInt(4, guide.getStock());

            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new IllegalStateException("Could not generate guide ID");
    }

    public Guide findById(long id) throws Exception {
        try (Connection connection = database.getConnection()) {
            return findById(connection, id);
        }
    }

    public Guide findById(Connection connection, long id) throws Exception {
        String sql = """
                SELECT id, name, current_price, default_unit_cost, stock
                FROM guides
                WHERE id = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                return new Guide(
                        result.getLong("id"),
                        result.getString("name"),
                        result.getBigDecimal("current_price"),
                        result.getBigDecimal("default_unit_cost"),
                        result.getInt("stock")
                );
            }
        }
    }

    public void update(Guide guide) throws Exception {
        try (Connection connection = database.getConnection()) {
            update(connection, guide);
        }
    }

    public void update(Connection connection, Guide guide) throws Exception {
        String sql = """
                UPDATE guides
                SET name = ?, current_price = ?, default_unit_cost = ?, stock = ?
                WHERE id = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, guide.getName());
            statement.setBigDecimal(2, guide.getCurrentPrice());
            statement.setBigDecimal(3, guide.getDefaultUnitCost());
            statement.setInt(4, guide.getStock());
            statement.setLong(5, guide.getId());

            statement.executeUpdate();
        }
    }

    public List<Guide> findAll() throws Exception {
        try (Connection connection = database.getConnection()) {
            return findAll(connection);
        }
    }

    public void updateDefaultUnitCosts(Map<Long, BigDecimal> costs) throws Exception {
        if (costs == null || costs.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un costo unitario");
        }
        database.inTransaction(connection -> {
            try (var statement = connection.prepareStatement(
                    "UPDATE guides SET default_unit_cost = ? WHERE id = ?")) {
                for (var entry : costs.entrySet()) {
                    BigDecimal cost = entry.getValue();
                    if (entry.getKey() == null || cost == null || cost.signum() < 0) {
                        throw new IllegalArgumentException("Los costos unitarios deben ser cero o mayores");
                    }
                    statement.setBigDecimal(1, cost);
                    statement.setLong(2, entry.getKey());
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException("No se encontró una de las guías");
                    }
                }
            }
            return null;
        });
    }

    public List<Guide> findAll(Connection connection) throws Exception {
        String sql = """
                SELECT id, name, current_price, default_unit_cost, stock
                FROM guides
                ORDER BY CASE name
                    WHEN 'Física General' THEN 1
                    WHEN 'Física I' THEN 2
                    WHEN 'Física II' THEN 3
                    WHEN 'Física III' THEN 4
                    ELSE 5
                END, id ASC
                """;
        List<Guide> guides = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                guides.add(new Guide(
                        result.getLong("id"),
                        result.getString("name"),
                        result.getBigDecimal("current_price"),
                        result.getBigDecimal("default_unit_cost"),
                        result.getInt("stock")
                ));
            }
        }
        return guides;
    }
}
