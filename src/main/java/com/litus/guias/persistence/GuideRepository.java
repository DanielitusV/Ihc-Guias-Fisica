package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;

import java.sql.Connection;
import java.sql.Statement;

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
                INSERT INTO guides (name, current_price, stock)
                VALUES (?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setString(1, guide.getName());
            statement.setBigDecimal(2, guide.getCurrentPrice());
            statement.setInt(3, guide.getStock());

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
                SELECT id, name, current_price, stock
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
                SET name = ?, current_price = ?, stock = ?
                WHERE id = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, guide.getName());
            statement.setBigDecimal(2, guide.getCurrentPrice());
            statement.setInt(3, guide.getStock());
            statement.setLong(4, guide.getId());

            statement.executeUpdate();
        }
    }
}
