package com.litus.guias.persistence;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Database {

    public static final int CURRENT_SCHEMA_VERSION = 4;

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
                    ensureAcademicTerms(connection);
                    ensureGuideDefaultCost(connection);
                    ensureOrderLifecycle(connection);
                    ensureOrderItemsAllowZeroCost(connection);
                    ensureColumn(connection, "inventory_adjustments", "cash_closure_id",
                            "INTEGER REFERENCES cash_closures(id)");
                    ensureMovementTransferConcept(connection);
                    ensureOperationalDates(connection);
                    statement.execute("""
                            CREATE INDEX IF NOT EXISTS idx_sales_account_id
                            ON sales(account_id)
                            """);
                    statement.execute("PRAGMA user_version = " + CURRENT_SCHEMA_VERSION);
                    connection.commit();
                } catch (Exception exception) {
                    connection.rollback();
                    throw exception;
                }
            }
        }
    }

    public int userVersion() throws Exception {
        try (Connection connection = getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA user_version")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    public String integrityCheck() throws Exception {
        try (Connection connection = getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA integrity_check")) {
            if (!result.next()) return "sin resultado";
            String first = result.getString(1);
            if ("ok".equalsIgnoreCase(first) && !result.next()) return "ok";
            StringBuilder problems = new StringBuilder(first);
            while (result.next()) problems.append("; ").append(result.getString(1));
            return problems.toString();
        }
    }

    public void requireIntegrity() throws Exception {
        String result = integrityCheck();
        if (!"ok".equals(result)) {
            throw new IllegalStateException("La base de datos no superó la verificación de integridad: " + result);
        }
    }

    public void backupTo(Path destination) throws Exception {
        Path absolute = destination.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        if (Files.exists(absolute)) throw new IllegalStateException("El respaldo ya existe: " + absolute);
        String escaped = absolute.toString().replace("'", "''");
        try (Connection connection = getConnection();
             var statement = connection.createStatement()) {
            statement.execute("VACUUM INTO '" + escaped + "'");
        }
        new Database("jdbc:sqlite:" + absolute).requireIntegrity();
    }

    private void ensureGuideDefaultCost(Connection connection) throws Exception {
        ensureColumn(connection, "guides", "default_unit_cost",
                "NUMERIC NOT NULL DEFAULT 0 CHECK (default_unit_cost >= 0)");
    }

    private void ensureOrderLifecycle(Connection connection) throws Exception {
        ensureColumn(connection, "orders", "status", "TEXT NOT NULL DEFAULT 'ACTIVE'");
        ensureColumn(connection, "orders", "cancellation_reason", "TEXT");
        ensureColumn(connection, "orders", "cancelled_at", "TEXT");
        ensureColumn(connection, "orders", "replacement_order_id", "INTEGER REFERENCES orders(id)");
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE orders SET status = 'ACTIVE' WHERE status IS NULL");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)");
        }
    }

    private void ensureOrderItemsAllowZeroCost(Connection connection) throws Exception {
        String definition = "";
        try (var statement = connection.prepareStatement(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'order_items'");
             var result = statement.executeQuery()) {
            if (result.next()) definition = result.getString(1);
        }
        if (definition != null && definition.replaceAll("\\s+", " ").contains("unit_cost >= 0")) return;
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE order_items RENAME TO order_items_legacy");
            statement.execute("""
                    CREATE TABLE order_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        order_id INTEGER NOT NULL,
                        guide_id INTEGER NOT NULL,
                        quantity INTEGER NOT NULL CHECK (quantity > 0),
                        unit_cost NUMERIC NOT NULL CHECK (unit_cost >= 0),
                        FOREIGN KEY (order_id) REFERENCES orders(id),
                        FOREIGN KEY (guide_id) REFERENCES guides(id)
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO order_items (id, order_id, guide_id, quantity, unit_cost)
                    SELECT id, order_id, guide_id, quantity, unit_cost
                    FROM order_items_legacy
                    """);
            statement.execute("DROP TABLE order_items_legacy");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_order_items_guide_id ON order_items(guide_id)");
        }
    }

    private void ensureMovementTransferConcept(Connection connection) throws Exception {
        String definition = "";
        try (var statement = connection.prepareStatement(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'account_movements'");
             var result = statement.executeQuery()) {
            if (result.next()) definition = result.getString(1);
        }
        if (definition != null && definition.contains("'TRANSFER'")) return;
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE account_movements RENAME TO account_movements_legacy");
            statement.execute("""
                    CREATE TABLE account_movements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        account_id INTEGER NOT NULL,
                        type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
                        concept TEXT NOT NULL CHECK (concept IN ('SALE', 'GENERAL_EXPENSE', 'SUPPLIER_PAYMENT', 'SALE_CANCELLATION', 'CLOSURE_ADJUSTMENT', 'TRANSFER', 'OTHER')),
                        amount NUMERIC NOT NULL CHECK (amount > 0),
                        reason TEXT,
                        created_at TEXT NOT NULL,
                        academic_term_id INTEGER,
                        FOREIGN KEY (account_id) REFERENCES accounts(id),
                        FOREIGN KEY (academic_term_id) REFERENCES academic_terms(id)
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO account_movements
                    (id, account_id, type, concept, amount, reason, created_at, academic_term_id)
                    SELECT id, account_id, type, concept, amount, reason, created_at, academic_term_id
                    FROM account_movements_legacy
                    """);
            statement.execute("DROP TABLE account_movements_legacy");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_account_movements_account_id ON account_movements(account_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_account_movements_created_at ON account_movements(created_at)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_movements_academic_term_id ON account_movements(academic_term_id)");
        }
    }

    private void ensureOperationalDates(Connection connection) throws Exception {
        ensureColumn(connection, "sales", "cancelled_at", "TEXT");
        ensureColumn(connection, "cash_closures", "closure_date", "TEXT");
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE cash_closures
                    SET closure_date = substr(created_at, 1, 10)
                    WHERE closure_date IS NULL
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_cash_closures_closure_date
                    ON cash_closures(closure_date)
                    """);
        }
    }

    private void ensureAcademicTerms(Connection connection) throws Exception {
        ensureColumn(connection, "sales", "academic_term_id",
                "INTEGER REFERENCES academic_terms(id)");
        ensureColumn(connection, "account_movements", "academic_term_id",
                "INTEGER REFERENCES academic_terms(id)");
        ensureColumn(connection, "orders", "academic_term_id",
                "INTEGER REFERENCES academic_terms(id)");
        ensureColumn(connection, "cash_closures", "academic_term_id",
                "INTEGER REFERENCES academic_terms(id)");

        Long activeId = null;
        int termCount;
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT COUNT(*) FROM academic_terms")) {
            result.next();
            termCount = result.getInt(1);
        }
        try (var statement = connection.createStatement();
             var result = statement.executeQuery(
                     "SELECT id FROM academic_terms WHERE status = 'OPEN'")) {
            if (result.next()) activeId = result.getLong(1);
        }
        if (activeId == null && termCount == 0) {
            String code = AcademicTermService.defaultCode(LocalDate.now());
            try (var statement = connection.prepareStatement("""
                    INSERT INTO academic_terms(code, status, opened_at)
                    VALUES (?, 'OPEN', ?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, code);
                statement.setString(2, LocalDateTime.now().toString());
                statement.executeUpdate();
                try (var keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) throw new IllegalStateException("Could not create academic term");
                    activeId = keys.getLong(1);
                }
            } catch (SQLException duplicate) {
                try (var statement = connection.prepareStatement(
                        "SELECT id FROM academic_terms WHERE code = ?")) {
                    statement.setString(1, code);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) throw duplicate;
                        activeId = result.getLong(1);
                    }
                }
            }
        }
        if (activeId != null) {
            for (String table : new String[]{"sales", "account_movements", "orders", "cash_closures", "inventory_adjustments"}) {
                try (var statement = connection.prepareStatement(
                        "UPDATE " + table + " SET academic_term_id = ? WHERE academic_term_id IS NULL")) {
                    statement.setLong(1, activeId);
                    statement.executeUpdate();
                }
            }
        }
        try (var statement = connection.createStatement()) {
            statement.execute("CREATE INDEX IF NOT EXISTS idx_sales_academic_term_id ON sales(academic_term_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_movements_academic_term_id ON account_movements(academic_term_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_orders_academic_term_id ON orders(academic_term_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_closures_academic_term_id ON cash_closures(academic_term_id)");
        }
    }

    private void ensureColumn(Connection connection, String table, String column, String definition)
            throws Exception {
        boolean found = false;
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equals(result.getString("name"))) { found = true; break; }
            }
        }
        if (!found) {
            try (var statement = connection.createStatement()) {
                statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
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
