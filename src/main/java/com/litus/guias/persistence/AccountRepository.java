package com.litus.guias.persistence;

import com.litus.guias.account.Account;

import java.sql.Connection;
import java.sql.Statement;

public class AccountRepository {

    private final Database database;

    public AccountRepository(Database database) {
        this.database = database;
    }

    public long save(Account account) throws Exception {
        try (Connection connection = database.getConnection()) {
            return save(connection, account);
        }
    }

    public long save(Connection connection, Account account) throws Exception {
        String sql = """
                INSERT INTO accounts (name, balance)
                VALUES (?, ?)
                """;

        try (var statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setString(1, account.getName());
            statement.setBigDecimal(2, account.getBalance());

            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new IllegalStateException("Could not generate account ID");
    }

    public Account findById(long id) throws Exception {
        try (Connection connection = database.getConnection()) {
            return findById(connection, id);
        }
    }

    public Account findById(Connection connection, long id) throws Exception {
        String sql = """
                SELECT id, name, balance
                FROM accounts
                WHERE id = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                return new Account(
                        result.getLong("id"),
                        result.getString("name"),
                        result.getBigDecimal("balance")
                );
            }
        }
    }

    public void update(Account account) throws Exception {
        try (Connection connection = database.getConnection()) {
            update(connection, account);
        }
    }

    public void update(Connection connection, Account account) throws Exception {
        String sql = """
                UPDATE accounts
                SET name = ?, balance = ?
                WHERE id = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, account.getName());
            statement.setBigDecimal(2, account.getBalance());
            statement.setLong(3, account.getId());

            statement.executeUpdate();
        }
    }
}