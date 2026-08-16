package com.litus.guias.persistence;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {

    private final String url;

    public Database(String url) {
        this.url = url;
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);

        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;");
        }

        return connection;
    }

    public void initialize() throws Exception {
        try (var input = Database.class.getResourceAsStream("/schema.sql")) {

            if (input == null) {
                throw new IllegalStateException("schema.sql not found");
            }

            String sql = new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            try (Connection connection = getConnection();
                 var statement = connection.createStatement()) {

                for (String command : sql.split(";")) {
                    if (!command.isBlank()) {
                        statement.execute(command);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    public interface TransactionWork<T> {
        T execute(Connection connection) throws Exception;
    }

    public <T> T inTransaction(TransactionWork<T> work) throws Exception {
        try (Connection connection = getConnection()) {

            connection.setAutoCommit(false);

            try {
                T result = work.execute(connection);

                connection.commit();

                return result;

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }
}