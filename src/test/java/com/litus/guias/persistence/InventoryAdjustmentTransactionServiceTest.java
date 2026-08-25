package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import com.litus.guias.inventory.InventoryAdjustmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryAdjustmentTransactionServiceTest {
    @TempDir Path tempDir;
    private Database database;
    private GuideRepository guides;
    private long guideId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
        guides = new GuideRepository(database);
        guideId = guides.save(new Guide(0, "Física II", new BigDecimal("35"), 0));
    }

    @Test
    void addsMissingStockWithoutCreatingOrderOrDebt() throws Exception {
        InventoryAdjustmentTransactionService service =
                new InventoryAdjustmentTransactionService(database);

        long id = service.register(
                guideId, 20, InventoryAdjustmentType.OMITTED_STOCK,
                "Remanente no registrado", LocalDateTime.of(2026, 8, 25, 9, 0)
        );

        assertEquals(20, guides.findById(guideId).getStock());
        assertEquals(1, new InventoryAdjustmentRepository(database).findAll().size());
        assertEquals(id, new InventoryAdjustmentRepository(database).findAll().getFirst().id());
        assertEquals(BigDecimal.ZERO, new SupplierDebtQueryService(database).calculateCurrentDebt());
    }

    @Test
    void rejectsAdjustmentThatWouldCreateNegativeStock() {
        InventoryAdjustmentTransactionService service =
                new InventoryAdjustmentTransactionService(database);

        assertThrows(IllegalStateException.class, () -> service.register(
                guideId, -1, InventoryAdjustmentType.COUNT_CORRECTION,
                "Faltante", LocalDateTime.now()));
    }
}
