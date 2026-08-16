package com.litus.guias.persistence;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class ApplicationBootstrap {

    public static final Set<String> GUIDE_NAMES = Set.of(
            "Física General",
            "Física I",
            "Física II",
            "Física III"
    );

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
        database.initialize();
        database.inTransaction(connection -> {
            String sql = """
                    INSERT INTO accounts (name, balance)
                    VALUES (?, 0)
                    ON CONFLICT(name) DO NOTHING
                    """;
            try (var statement = connection.prepareStatement(sql)) {
                for (String name : new String[]{"Efectivo", "QR / Soto"}) {
                    statement.setString(1, name);
                    statement.executeUpdate();
                }
            }
            return null;
        });
        return database;
    }

    public void initializeGuides(Map<String, BigDecimal> prices)
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

        initialize();
        database.inTransaction(connection -> {
            String sql = """
                    INSERT INTO guides (name, current_price, stock)
                    VALUES (?, ?, 0)
                    ON CONFLICT(name) DO NOTHING
                    """;
            try (var statement = connection.prepareStatement(sql)) {
                for (String name : GUIDE_NAMES) {
                    statement.setString(1, name);
                    statement.setBigDecimal(2, prices.get(name));
                    statement.executeUpdate();
                }
            }
            return null;
        });
    }
}
