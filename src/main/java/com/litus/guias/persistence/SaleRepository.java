package com.litus.guias.persistence;

import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.SaleStatus;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;

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
                (guide_id, price, payment_method, status,
                 cancellation_reason, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
        )) {

            statement.setLong(1, sale.getGuideId());
            statement.setBigDecimal(2, sale.getPrice());
            statement.setString(3, sale.getPaymentMethod().name());
            statement.setString(4, sale.getStatus().name());
            statement.setString(5, sale.getCancellationReason());
            statement.setString(6, sale.getCreatedAt().toString());

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
                SELECT id, guide_id, price, payment_method,
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

                Sale sale = new Sale(
                        result.getLong("id"),
                        result.getLong("guide_id"),
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
}
