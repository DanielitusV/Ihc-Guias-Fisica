package com.litus.guias.persistence;

import com.litus.guias.account.*;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

public class AccountMovementRepository {

    private final Database database;

    public AccountMovementRepository(Database database) {
        this.database = database;
    }

    public long save(AccountMovement movement) throws Exception {
        try (Connection connection = database.getConnection()) {
            return save(connection, movement);
        }
    }

    public long save(
            Connection connection,
            AccountMovement movement
    ) throws Exception {

        String sql = """
                INSERT INTO account_movements
                (account_id, type, concept, amount, reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setLong(1, movement.getAccountId());
            statement.setString(2, movement.getType().name());
            statement.setString(3, movement.getConcept().name());
            statement.setBigDecimal(4, movement.getAmount());
            statement.setString(5, movement.getReason());
            statement.setString(6, movement.getCreatedAt().toString());

            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new IllegalStateException(
                "Could not generate account movement ID"
        );
    }

    public AccountMovement findById(long id) throws Exception {
        try (Connection connection = database.getConnection()) {
            return findById(connection, id);
        }
    }

    public AccountMovement findById(
            Connection connection,
            long id
    ) throws Exception {

        String sql = """
                SELECT id, account_id, type, concept,
                       amount, reason, created_at
                FROM account_movements
                WHERE id = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                return new AccountMovement(
                        result.getLong("id"),
                        result.getLong("account_id"),
                        AccountMovementType.valueOf(
                                result.getString("type")
                        ),
                        AccountMovementConcept.valueOf(
                                result.getString("concept")
                        ),
                        result.getBigDecimal("amount"),
                        result.getString("reason"),
                        LocalDateTime.parse(
                                result.getString("created_at")
                        )
                );
            }
        }
    }
}