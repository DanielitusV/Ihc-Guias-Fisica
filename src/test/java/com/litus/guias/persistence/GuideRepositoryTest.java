package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

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
                10
        );

        long id = repository.save(guide);
        Guide found = repository.findById(id);

        assertNotNull(found);
        assertEquals(id, found.getId());
        assertEquals(10, found.getStock());

        assertEquals(0, new BigDecimal("35.00").compareTo(found.getCurrentPrice()));
    }

    @Test
    public void updatesGuides() throws Exception {
        GuideRepository repository = new GuideRepository(database);

        long id = repository.save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 10)
        );

        Guide guide = repository.findById(id);
        guide.sellOne();

        repository.update(guide);
        Guide updated = repository.findById(id);
        assertEquals(9, updated.getStock());
    }
}
