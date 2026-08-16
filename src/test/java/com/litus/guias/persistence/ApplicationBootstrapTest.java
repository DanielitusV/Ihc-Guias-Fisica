package com.litus.guias.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
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
        assertEquals(2, new AccountRepository(first).findAll().size());
        assertEquals(2, new AccountRepository(second).findAll().size());
        assertEquals(0, new GuideRepository(second).findAll().size());

        bootstrap.initializeGuides(Map.of(
                "Física General", new BigDecimal("30.00"),
                "Física I", new BigDecimal("35.00"),
                "Física II", new BigDecimal("40.00"),
                "Física III", new BigDecimal("45.00")
        ));
        bootstrap.initializeGuides(Map.of(
                "Física General", new BigDecimal("30.00"),
                "Física I", new BigDecimal("35.00"),
                "Física II", new BigDecimal("40.00"),
                "Física III", new BigDecimal("45.00")
        ));

        assertEquals(4, new GuideRepository(second).findAll().size());
    }
}
