package com.litus.guias.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationBootstrapTest {

    @TempDir
    Path tempDir;

    @Test
    void initializesPersistentDatabaseIdempotentlyWithoutInventingPrices()
            throws Exception {
        Path dataDirectory = tempDir.resolve("GuiasFisica");
        ApplicationBootstrap bootstrap = new ApplicationBootstrap(dataDirectory);

        Database first = bootstrap.initialize();
        Database second = bootstrap.initialize();

        assertTrue(dataDirectory.resolve("guias.db").toFile().isFile());
        assertTrue(Files.notExists(dataDirectory.resolve("Respaldos")));
        assertEquals(3, new AccountRepository(first).findAll().size());
        assertEquals(3, new AccountRepository(second).findAll().size());
        assertEquals("Cuenta del encargado", new AccountRepository(second).findAll().get(2).getName());
        assertEquals(0, new GuideRepository(second).findAll().size());

        Map<String, BigDecimal> prices = Map.of(
                "Física General", new BigDecimal("30.00"),
                "Física I", new BigDecimal("35.00"),
                "Física II", new BigDecimal("40.00"),
                "Física III", new BigDecimal("45.00")
        );
        Map<String, BigDecimal> costs = Map.of(
                "Física General", BigDecimal.ZERO,
                "Física I", new BigDecimal("18.00"),
                "Física II", new BigDecimal("19.00"),
                "Física III", new BigDecimal("20.00")
        );
        bootstrap.initializeGuides(prices, costs);
        bootstrap.initializeGuides(prices, costs);

        assertEquals(4, new GuideRepository(second).findAll().size());
        assertEquals(0, new BigDecimal("18.00").compareTo(
                new GuideRepository(second).findAll().get(1).getDefaultUnitCost()));
    }

    @Test
    void backsUpLegacyDatabaseBeforeMigratingWithoutLosingRows() throws Exception {
        Path dataDirectory = tempDir.resolve("GuiasFisicaLegacy");
        Files.createDirectories(dataDirectory);
        Database legacy = new Database("jdbc:sqlite:" + dataDirectory.resolve("guias.db"));
        try (var connection = legacy.getConnection();
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE guides (id INTEGER PRIMARY KEY, name TEXT UNIQUE, current_price NUMERIC, stock INTEGER)");
            statement.execute("CREATE TABLE accounts (id INTEGER PRIMARY KEY, name TEXT UNIQUE, balance NUMERIC)");
            statement.execute("CREATE TABLE sales (id INTEGER PRIMARY KEY, guide_id INTEGER, price NUMERIC, payment_method TEXT, status TEXT, cancellation_reason TEXT, created_at TEXT)");
            statement.execute("INSERT INTO guides VALUES (1, 'Física I', 35, 17)");
            statement.execute("INSERT INTO accounts VALUES (1, 'Efectivo', 350)");
            statement.execute("INSERT INTO sales VALUES (1, 1, 35, 'CASH', 'ACTIVE', NULL, '2026-08-18T10:00')");
        }

        Database migrated = new ApplicationBootstrap(dataDirectory).initialize();

        assertEquals(17, new GuideRepository(migrated).findById(1).getStock());
        assertEquals(1, new SaleRepository(migrated).findAll().size());
        assertEquals(3, new AccountRepository(migrated).findAll().size());
        assertEquals(Database.CURRENT_SCHEMA_VERSION, migrated.userVersion());
        assertEquals("ok", migrated.integrityCheck());

        java.util.List<Path> backups;
        try (var stream = Files.list(dataDirectory.resolve("Respaldos"))) {
            backups = stream.toList();
        }
        assertEquals(1, backups.size());
        Database backup = new Database("jdbc:sqlite:" + backups.getFirst());
        assertEquals(0, backup.userVersion());
        try (var connection = backup.getConnection();
             var result = connection.createStatement().executeQuery("SELECT stock FROM guides WHERE id = 1")) {
            assertTrue(result.next());
            assertEquals(17, result.getInt(1));
        }

        new ApplicationBootstrap(dataDirectory).initialize();
        try (var stream = Files.list(dataDirectory.resolve("Respaldos"))) {
            assertEquals(1, stream.count());
        }
    }
}
