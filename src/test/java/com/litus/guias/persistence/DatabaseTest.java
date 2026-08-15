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
                            "guides",
                            "sales",
                            "accounts",
                            "account_movements",
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
}
