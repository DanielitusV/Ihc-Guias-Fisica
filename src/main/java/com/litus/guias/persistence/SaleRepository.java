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

    public SaleRepository(Database database) {
        this.database = database;
    }

    public long save(Sale sale) throws Exception {
        try (Connection connection = database.getConnection()) {
            return save(connection, sale);
        }
    }

    public long save(Connection connection, Sale sale) throws Exception {
        String sql = """
                INSERT INTO sales
                (guide_id, account_id, price, payment_method, status,
                 cancellation_reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setLong(1, sale.getGuideId());
            statement.setLong(2, sale.getAccountId());
            statement.setBigDecimal(3, sale.getPrice());
            statement.setString(4, sale.getPaymentMethod().name());
            statement.setString(5, sale.getStatus().name());
            statement.setString(6, sale.getCancellationReason());
            statement.setString(7, sale.getCreatedAt().toString());

            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }

        throw new IllegalStateException("Could not generate sale ID");
    }

    public Sale findById(long id) throws Exception {
        try (Connection connection = database.getConnection()) {
            return findById(connection, id);
        }
    }

    public Sale findById(Connection connection, long id) throws Exception {
        String sql = """
                SELECT id, guide_id, account_id, price, payment_method,
                       status, cancellation_reason, created_at
                FROM sales
                WHERE id = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }

                return mapSale(result);
            }
        }
    }

    public void update(Sale sale) throws Exception {
        try (Connection connection = database.getConnection()) {
            update(connection, sale);
        }
    }

    public void update(Connection connection, Sale sale) throws Exception {
        String sql = """
                UPDATE sales
                SET status = ?, cancellation_reason = ?
                WHERE id = ?
                """;

        try (var statement = connection.prepareStatement(sql)) {

            statement.setString(1, sale.getStatus().name());
            statement.setString(2, sale.getCancellationReason());
            statement.setLong(3, sale.getId());

            statement.executeUpdate();
        }
    }

    public List<Sale> findAll() throws Exception {
        try (Connection connection = database.getConnection()) {
            return findAll(connection);
        }
    }

    public List<Sale> findAll(Connection connection) throws Exception {
        return findBetween(
                connection,
                LocalDateTime.of(1, 1, 1, 0, 0),
                LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999)
        );
    }

    public List<Sale> findBetween(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    ) throws Exception {
        try (Connection connection = database.getConnection()) {
            return findBetween(connection, startInclusive, endExclusive);
        }
    }

    public List<Sale> findBetween(
            Connection connection,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    ) throws Exception {
        String sql = """
                SELECT id, guide_id, account_id, price, payment_method,
                       status, cancellation_reason, created_at
                FROM sales
                WHERE created_at >= ? AND created_at < ?
                ORDER BY created_at ASC, id ASC
                """;
        List<Sale> sales = new ArrayList<>();
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, startInclusive.toString());
            statement.setString(2, endExclusive.toString());
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    sales.add(mapSale(result));
                }
            }
        }
        return sales;
    }

    private Sale mapSale(java.sql.ResultSet result) throws Exception {
        Sale sale = new Sale(
                result.getLong("id"),
                result.getLong("guide_id"),
                result.getLong("account_id"),
                result.getBigDecimal("price"),
                PaymentMethod.valueOf(result.getString("payment_method")),
                LocalDateTime.parse(result.getString("created_at")),
                SaleStatus.ACTIVE
        );
        if (SaleStatus.valueOf(result.getString("status"))
                == SaleStatus.CANCELLED) {
            sale.cancel(result.getString("cancellation_reason"));
        }
        return sale;
    }
}
