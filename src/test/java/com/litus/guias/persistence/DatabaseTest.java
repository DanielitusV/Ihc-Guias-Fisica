package com.litus.guias.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import java.sql.Connection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {
    @TempDir
    Path tempDir;

    private Database database;

    @BeforeEach
    public void setup() {
        Path dbPath = tempDir.resolve("test.db");
        database = new Database("jdbc:sqlite:" + dbPath);
    }

    /*
     * Tests starts here
     */

    @Test
    public void opensSQLiteConnection() throws Exception {
        try (Connection connection = database.getConnection()) {
            assertFalse(connection.isClosed());
        }
    }

    @Test
    public void initializesDatabaseSchema() throws Exception {
        database.initialize();

        assertEquals(Database.CURRENT_SCHEMA_VERSION, database.userVersion());
        assertEquals("ok", database.integrityCheck());

        try (Connection connection = database.getConnection()) {
            var statement = connection.prepareStatement(
                    """
                        SELECT name
                        FROM sqlite_master
                        WHERE type = 'table'
                        AND name = 'guides'
                        """
            );
            var result = statement.executeQuery();

            assertTrue(result.next());
        }
    }

    @Test
    public void createsAllRequiredTables() throws Exception {
        database.initialize();
        try (Connection connection = database.getConnection();
             var statement = connection.prepareStatement(
                     """
                         SELECT name
                         FROM sqlite_master
                         WHERE type = 'table'
                         AND name NOT LIKE 'sqlite_%'
                         """
             );
             var result = statement.executeQuery()) {

            var tables = new java.util.HashSet<String>();
            while (result.next()) {
                tables.add(result.getString("name"));
            }

            assertEquals(
                Set.of(
                        "academic_terms",
                            "guides",
                            "sales",
                            "accounts",
                            "account_movements",
                            "authorized_deliveries",
                            "authorized_delivery_returns",
                            "inventory_adjustments",
                            "orders",
                            "order_items",
                            "cash_closures",
                            "cash_closure_items"
                    ),
                    tables
            );
        }
    }

    @Test
    public void enablesForeignKeys() throws Exception {
        try (Connection connection = database.getConnection();
            var statement = connection.createStatement();
            var result = statement.executeQuery("PRAGMA foreign_keys;")) {

            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void addsPersistentDefaultCostToLegacyGuides() throws Exception {
        try (Connection connection = database.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE guides (id INTEGER PRIMARY KEY, name TEXT, current_price NUMERIC, stock INTEGER)");
            statement.execute("INSERT INTO guides VALUES (1, 'Física I', 35, 2)");
        }

        database.initialize();

        GuideRepository repository = new GuideRepository(database);
        assertEquals(0, repository.findById(1).getDefaultUnitCost().compareTo(java.math.BigDecimal.ZERO));
    }

    @Test
    void upgradesOrderItemsToAllowPendingCostWithoutDeletingRows() throws Exception {
        database.initialize();
        try (Connection connection = database.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("DROP TABLE order_items");
            statement.execute("""
                    CREATE TABLE order_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        order_id INTEGER NOT NULL,
                        guide_id INTEGER NOT NULL,
                        quantity INTEGER NOT NULL CHECK (quantity > 0),
                        unit_cost NUMERIC NOT NULL CHECK (unit_cost > 0),
                        FOREIGN KEY (order_id) REFERENCES orders(id),
                        FOREIGN KEY (guide_id) REFERENCES guides(id)
                    )
                    """);
            statement.execute("INSERT INTO guides(name, current_price, default_unit_cost, stock) VALUES ('Física II', 35, 20, 5)");
            statement.execute("INSERT INTO orders(payment_condition, created_at, status) VALUES ('CREDIT', '2026-08-25T10:00', 'ACTIVE')");
            statement.execute("INSERT INTO order_items(order_id, guide_id, quantity, unit_cost) VALUES (1, 1, 5, 20)");
            statement.execute("PRAGMA user_version = 3");
        }

        database.initialize();

        try (Connection connection = database.getConnection();
             var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT quantity, unit_cost FROM order_items WHERE id = 1")) {
                assertTrue(result.next());
                assertEquals(5, result.getInt("quantity"));
                assertEquals(0, result.getBigDecimal("unit_cost").compareTo(new java.math.BigDecimal("20")));
            }
            assertDoesNotThrow(() -> statement.execute(
                    "INSERT INTO order_items(order_id, guide_id, quantity, unit_cost) VALUES (1, 1, 2, 0)"));
        }
    }

    @Test
    void upgradesLegacySalesSchemaWithoutDeletingRows() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("guias.db"));
        try (Connection connection = database.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE guides (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE,
                        current_price NUMERIC NOT NULL,
                        stock INTEGER NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE accounts (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE,
                        balance NUMERIC NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE sales (
                        id INTEGER PRIMARY KEY,
                        guide_id INTEGER NOT NULL,
                        price NUMERIC NOT NULL,
                        payment_method TEXT NOT NULL,
                        status TEXT NOT NULL,
                        cancellation_reason TEXT,
                        created_at TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    INSERT INTO guides VALUES (1, 'Física I', 35, 9)
                    """);
            statement.execute("""
                    INSERT INTO accounts VALUES (1, 'Efectivo', 35)
                    """);
            statement.execute("""
                    INSERT INTO sales VALUES
                    (1, 1, 35, 'CASH', 'ACTIVE', NULL, '2026-08-15T10:00')
                    """);
        }

        new ApplicationBootstrap(tempDir).initialize();

        try (Connection connection = database.getConnection();
             var columns = connection.createStatement()
                     .executeQuery("PRAGMA table_info(sales)")) {
            boolean hasAccountId = false;
            while (columns.next()) {
                hasAccountId |= "account_id".equals(columns.getString("name"));
            }
            assertTrue(hasAccountId);
        }
        try (Connection connection = database.getConnection();
             var result = connection.createStatement()
                     .executeQuery("SELECT COUNT(*), account_id FROM sales")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
            assertEquals(1, result.getLong("account_id"));
        }
    }
}
