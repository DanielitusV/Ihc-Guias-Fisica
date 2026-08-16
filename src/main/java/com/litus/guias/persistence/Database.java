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
                connection.setAutoCommit(false);
                try {
                    for (String command : sql.split(";")) {
                        if (!command.isBlank()) {
                            statement.execute(command);
                        }
                    }
                    ensureSaleAccountColumn(connection);
                    statement.execute("""
                            CREATE INDEX IF NOT EXISTS idx_sales_account_id
                            ON sales(account_id)
                            """);
                    connection.commit();
                } catch (Exception exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        }
    }

    private void ensureSaleAccountColumn(Connection connection)
            throws Exception {
        boolean found = false;
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA table_info(sales)")) {
            while (result.next()) {
                if ("account_id".equals(result.getString("name"))) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            try (var statement = connection.createStatement()) {
                statement.execute("""
                        ALTER TABLE sales
                        ADD COLUMN account_id INTEGER REFERENCES accounts(id)
                        """);
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
