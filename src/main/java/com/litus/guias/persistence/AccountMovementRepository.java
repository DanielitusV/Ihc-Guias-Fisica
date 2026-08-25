package com.litus.guias.persistence;

import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.AccountMovementConcept;
import com.litus.guias.account.AccountMovementType;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccountMovementRepository {

    private final Database database;

    public AccountMovementRepository(Database database) {
        this.database = database;
    }

    public long save(
            AccountMovement movement
    ) throws Exception {

        try (Connection connection =
                     database.getConnection()) {

            return save(connection, movement);
        }
    }

    public long save(
            Connection connection,
            AccountMovement movement
    ) throws Exception {

        String sql = """
                INSERT INTO account_movements
                (
                    account_id,
                    type,
                    concept,
                    amount,
                    reason,
                    created_at,
                    academic_term_id
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setLong(
                    1,
                    movement.getAccountId()
            );

            statement.setString(
                    2,
                    movement.getType().name()
            );

            statement.setString(
                    3,
                    movement.getConcept().name()
            );

            statement.setBigDecimal(
                    4,
                    movement.getAmount()
            );

            statement.setString(
                    5,
                    movement.getReason()
            );

            statement.setString(
                    6,
                    movement.getCreatedAt().toString()
            );
            statement.setLong(7, AcademicTermRepository.requireActiveId(connection));

            statement.executeUpdate();

            try (var keys =
                         statement.getGeneratedKeys()) {

                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new IllegalStateException(
                "Could not generate account movement ID"
        );
    }

    public AccountMovement findById(
            long id
    ) throws Exception {

        try (Connection connection =
                     database.getConnection()) {

            return findById(connection, id);
        }
    }

    public AccountMovement findById(
            Connection connection,
            long id
    ) throws Exception {

        String sql = """
                SELECT
                    id,
                    account_id,
                    type,
                    concept,
                    amount,
                    reason,
                    created_at
                FROM account_movements
                WHERE id = ?
                """;

        try (var statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (var result =
                         statement.executeQuery()) {

                if (!result.next()) {
                    return null;
                }

                return mapMovement(result);
            }
        }
    }

    public List<AccountMovement> findSupplierPayments()
            throws Exception {

        try (Connection connection =
                     database.getConnection()) {

            return findSupplierPayments(connection);
        }
    }

    public List<AccountMovement> findSupplierPayments(
            Connection connection
    ) throws Exception {
        Long activeId = AcademicTermRepository.activeIdOrNull(connection);
        if (activeId == null) return List.of();

        String sql = """
                SELECT
                    id,
                    account_id,
                    type,
                    concept,
                    amount,
                    reason,
                    created_at
                FROM account_movements
                WHERE concept = 'SUPPLIER_PAYMENT' AND academic_term_id = ?
                ORDER BY created_at ASC
                """;

        List<AccountMovement> movements =
                new ArrayList<>();

        try (var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, activeId);
            try (var result = statement.executeQuery()) {
                while (result.next()) movements.add(mapMovement(result));
            }
        }

        return movements;
    }

    public List<AccountMovement> findAll() throws Exception {
        try (Connection connection = database.getConnection()) {
            Long activeId = AcademicTermRepository.activeIdOrNull(connection);
            return activeId == null ? List.of() : query(connection, activeId, null, null, null, null);
        }
    }

    public List<AccountMovement> findAllByTerm(long academicTermId) throws Exception {
        try (Connection connection = database.getConnection()) {
            return query(connection, academicTermId, null, null, null, null);
        }
    }

    public List<AccountMovement> findByConcept(
            AccountMovementConcept concept
    ) throws Exception {
        try (Connection connection = database.getConnection()) {
            Long activeId = AcademicTermRepository.activeIdOrNull(connection);
            return activeId == null ? List.of() : query(connection, activeId, null, concept, null, null);
        }
    }

    public List<AccountMovement> findByAccountId(long accountId)
            throws Exception {
        try (Connection connection = database.getConnection()) {
            Long activeId = AcademicTermRepository.activeIdOrNull(connection);
            return activeId == null ? List.of() : query(connection, activeId, accountId, null, null, null);
        }
    }

    public List<AccountMovement> findBetween(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    ) throws Exception {
        try (Connection connection = database.getConnection()) {
            Long activeId = AcademicTermRepository.activeIdOrNull(connection);
            if (activeId == null) return List.of();
            return query(
                    connection,
                    activeId,
                    null,
                    null,
                    startInclusive,
                    endExclusive
            );
        }
    }

    public List<AccountMovement> findBetweenByTerm(long academicTermId,
            LocalDateTime startInclusive, LocalDateTime endExclusive) throws Exception {
        try (Connection connection = database.getConnection()) {
            return query(connection, academicTermId, null, null, startInclusive, endExclusive);
        }
    }

    private List<AccountMovement> query(
            Connection connection,
            long academicTermId,
            Long accountId,
            AccountMovementConcept concept,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    ) throws Exception {
        StringBuilder sql = new StringBuilder("""
                SELECT id, account_id, type, concept, amount, reason, created_at
                FROM account_movements
                WHERE academic_term_id = ?
                """);
        if (concept != null) {
            sql.append(" AND concept = ?");
        }
        if (accountId != null) {
            sql.append(" AND account_id = ?");
        }
        if (startInclusive != null) {
            sql.append(" AND created_at >= ? AND created_at < ?");
        }
        sql.append(" ORDER BY created_at ASC, id ASC");

        List<AccountMovement> movements = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql.toString())) {
            int parameter = 1;
            statement.setLong(parameter++, academicTermId);
            if (concept != null) {
                statement.setString(parameter++, concept.name());
            }
            if (accountId != null) {
                statement.setLong(parameter++, accountId);
            }
            if (startInclusive != null) {
                statement.setString(parameter++, startInclusive.toString());
                statement.setString(parameter, endExclusive.toString());
            }
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    movements.add(mapMovement(result));
                }
            }
        }
        return movements;
    }

    private AccountMovement mapMovement(
            java.sql.ResultSet result
    ) throws Exception {

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
