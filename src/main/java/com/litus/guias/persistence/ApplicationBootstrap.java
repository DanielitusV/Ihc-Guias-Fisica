package com.litus.guias.persistence;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.Set;

public class ApplicationBootstrap {

    private static final String RELEASE_VERSION = "1.0.11";
    private static final DateTimeFormatter BACKUP_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    public static final List<String> GUIDE_ORDER = List.of(
            "Física General",
            "Física I",
            "Física II",
            "Física III"
    );
    public static final Set<String> GUIDE_NAMES = Set.copyOf(GUIDE_ORDER);

    private final Path dataDirectory;
    private final Database database;

    public ApplicationBootstrap() {
        this(ApplicationDataPath.resolve());
    }

    public ApplicationBootstrap(Path dataDirectory) {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
        this.database = new Database(
                "jdbc:sqlite:" + this.dataDirectory.resolve("guias.db")
        );
    }

    public Database initialize() throws Exception {
        Files.createDirectories(dataDirectory);
        Path databaseFile = dataDirectory.resolve("guias.db");
        if (Files.isRegularFile(databaseFile)) {
            database.requireIntegrity();
            int installedSchema = database.userVersion();
            if (installedSchema > Database.CURRENT_SCHEMA_VERSION) {
                throw new IllegalStateException(
                        "La base de datos pertenece a una versión más nueva de Guías Física");
            }
            if (installedSchema < Database.CURRENT_SCHEMA_VERSION) {
                Path backup = dataDirectory.resolve("Respaldos").resolve(
                        "guias-antes-" + RELEASE_VERSION + "-"
                                + LocalDateTime.now().format(BACKUP_TIME) + ".db");
                database.backupTo(backup);
            }
        }
        database.initialize();
        database.inTransaction(connection -> {
            String sql = """
                    INSERT INTO accounts (name, balance)
                    VALUES (?, 0)
                    ON CONFLICT(name) DO NOTHING
                    """;
            try (var statement = connection.prepareStatement(sql)) {
                for (String name : new String[]{"Efectivo", "QR / Soto", "Cuenta del encargado"}) {
                    statement.setString(1, name);
                    statement.executeUpdate();
                }
            }
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        UPDATE sales
                        SET account_id = CASE payment_method
                            WHEN 'CASH' THEN (SELECT id FROM accounts WHERE name = 'Efectivo')
                            WHEN 'QR' THEN (SELECT id FROM accounts WHERE name = 'QR / Soto')
                        END
                        WHERE account_id IS NULL
                        """);
            }
            return null;
        });
        database.requireIntegrity();
        return database;
    }

    public void initializeGuides(Map<String, BigDecimal> prices)
            throws Exception {
        Map<String, BigDecimal> costs = new java.util.LinkedHashMap<>();
        GUIDE_ORDER.forEach(name -> costs.put(name, BigDecimal.ZERO));
        initializeGuides(prices, costs);
    }

    public void initializeGuides(Map<String, BigDecimal> prices, Map<String, BigDecimal> costs)
            throws Exception {
        if (prices == null || !prices.keySet().equals(GUIDE_NAMES)) {
            throw new IllegalArgumentException(
                    "Prices for all four canonical guides are required"
            );
        }
        for (BigDecimal price : prices.values()) {
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Guide prices must be greater than zero"
                );
            }
        }
        if (costs == null || !costs.keySet().equals(GUIDE_NAMES)) {
            throw new IllegalArgumentException("Costs for all four canonical guides are required");
        }
        for (BigDecimal cost : costs.values()) {
            if (cost == null || cost.signum() < 0) {
                throw new IllegalArgumentException("Guide costs must be zero or greater");
            }
        }

        initialize();
        database.inTransaction(connection -> {
            String sql = """
                    INSERT INTO guides (name, current_price, default_unit_cost, stock)
                    VALUES (?, ?, ?, 0)
                    ON CONFLICT(name) DO UPDATE SET
                        current_price = excluded.current_price,
                        default_unit_cost = excluded.default_unit_cost
                    """;
            try (var statement = connection.prepareStatement(sql)) {
                for (String name : GUIDE_ORDER) {
                    statement.setString(1, name);
                    statement.setBigDecimal(2, prices.get(name));
                    statement.setBigDecimal(3, costs.get(name));
                    statement.executeUpdate();
                }
            }
            return null;
        });
    }
}
