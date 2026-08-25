package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import com.litus.guias.inventory.InventoryAdjustment;
import com.litus.guias.inventory.InventoryAdjustmentType;

import java.time.LocalDateTime;

public final class InventoryAdjustmentTransactionService {
    private final Database database;
    private final GuideRepository guides;
    private final InventoryAdjustmentRepository adjustments;

    public InventoryAdjustmentTransactionService(Database database) {
        this.database = database;
        guides = new GuideRepository(database);
        adjustments = new InventoryAdjustmentRepository(database);
    }

    public long register(long guideId, int quantityDelta, InventoryAdjustmentType type,
            String reason, LocalDateTime createdAt) throws Exception {
        InventoryAdjustment adjustment = new InventoryAdjustment(
                0, guideId, quantityDelta, type, reason, createdAt);
        return database.inTransaction(connection -> {
            Guide guide = guides.findById(connection, adjustment.guideId());
            if (guide == null) throw new IllegalArgumentException("No se encontró la guía");
            applyDelta(guide, adjustment.quantityDelta());
            guides.update(connection, guide);
            return adjustments.save(connection, adjustment);
        });
    }

    static void applyDelta(Guide guide, int quantityDelta) {
        if (quantityDelta > 0) guide.addStock(quantityDelta);
        else guide.removeStock(Math.negateExact(quantityDelta));
    }
}
