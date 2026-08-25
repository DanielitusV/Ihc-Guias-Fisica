package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GuideRepositoryTest {

    @TempDir
    Path tempDir;

    private Database database;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
    }

    /*
     * Tests start here
     */

    @Test
    public void savesAndFindsGuide() throws Exception {
        GuideRepository repository = new GuideRepository(database);
        Guide guide = new Guide(
                0,
                "Física I",
                new BigDecimal("35.00"),
                new BigDecimal("18.50"),
                10
        );

        long id = repository.save(guide);
        Guide found = repository.findById(id);

        assertNotNull(found);
        assertEquals(id, found.getId());
        assertEquals(10, found.getStock());

        assertEquals(0, new BigDecimal("35.00").compareTo(found.getCurrentPrice()));
        assertEquals(0, new BigDecimal("18.50").compareTo(found.getDefaultUnitCost()));
    }

    @Test
    public void updatesGuides() throws Exception {
        GuideRepository repository = new GuideRepository(database);

        long id = repository.save(
                new Guide(0, "Física I", new BigDecimal("35.00"), new BigDecimal("18.50"), 10)
        );

        Guide guide = repository.findById(id);
        guide.sellOne();
        guide.setDefaultUnitCost(new BigDecimal("19.25"));

        repository.update(guide);
        Guide updated = repository.findById(id);
        assertEquals(9, updated.getStock());
        assertEquals(0, new BigDecimal("19.25").compareTo(updated.getDefaultUnitCost()));
    }

    @Test
    public void updatesSeveralDefaultUnitCostsPersistently() throws Exception {
        GuideRepository repository = new GuideRepository(database);
        long first = repository.save(
                new Guide(0, "Física I", new BigDecimal("35.00"), BigDecimal.ZERO, 10));
        long second = repository.save(
                new Guide(0, "Física II", new BigDecimal("35.00"), BigDecimal.ZERO, 10));

        repository.updateDefaultUnitCosts(Map.of(
                first, new BigDecimal("17.50"),
                second, new BigDecimal("19.25")
        ));

        assertEquals(0, new BigDecimal("17.50").compareTo(repository.findById(first).getDefaultUnitCost()));
        assertEquals(0, new BigDecimal("19.25").compareTo(repository.findById(second).getDefaultUnitCost()));
    }
}
