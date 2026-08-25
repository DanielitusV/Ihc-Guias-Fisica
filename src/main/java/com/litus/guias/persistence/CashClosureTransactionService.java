package com.litus.guias.persistence;

import com.litus.guias.account.CashClosureItem;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.closure.CashClosureService;
import com.litus.guias.inventory.Guide;
import com.litus.guias.inventory.InventoryAdjustment;
import com.litus.guias.inventory.InventoryAdjustmentType;

import java.util.List;

public class CashClosureTransactionService {

    private final Database database;
    private final CashClosureRepository repository;
    private final CashClosureService closureService;
    private final GuideRepository guides;
    private final InventoryAdjustmentRepository adjustments;

    public CashClosureTransactionService(Database database) {
        this.database = database;
        this.repository = new CashClosureRepository(database);
        this.closureService = new CashClosureService();
        guides = new GuideRepository(database);
        adjustments = new InventoryAdjustmentRepository(database);
    }

    public long registerClosure(
            CashClosure closure,
            List<CashClosureItem> items
    ) throws Exception {
        return registerClosure(closure, items, false, null);
    }

    public long registerClosure(
            CashClosure closure,
            List<CashClosureItem> items,
            boolean reconcileInventory,
            String adjustmentReason
    ) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cash closure must contain at least one item"
            );
        }
        if (reconcileInventory && (adjustmentReason == null || adjustmentReason.isBlank())) {
            throw new IllegalArgumentException("El motivo del ajuste es obligatorio");
        }

        return database.inTransaction(connection -> {
            List<CashClosure> existing = repository.findAllByDate(
                    connection,
                    closure.getBusinessDate()
            );
            closureService.registerClosure(closure, existing);
            long closureId = repository.save(connection, closure, items);
            if (reconcileInventory) {
                for (CashClosureItem item : items) {
                    int delta = item.getCountedStock() - item.getExpectedStock();
                    if (delta == 0) continue;
                    Guide guide = guides.findById(connection, item.getGuideId());
                    if (guide == null) throw new IllegalArgumentException("No se encontró una guía del cierre");
                    InventoryAdjustmentTransactionService.applyDelta(guide, delta);
                    guides.update(connection, guide);
                    adjustments.save(connection, new InventoryAdjustment(
                            0, item.getGuideId(), delta, InventoryAdjustmentType.COUNT_CORRECTION,
                            adjustmentReason.trim(), closure.getBusinessDate().atTime(23, 59, 59), closureId
                    ));
                }
            }
            return closureId;
        });
    }

    public void cancelClosure(long closureId, String reason) throws Exception {
        database.inTransaction(connection -> {
            CashClosure closure = repository.findById(connection, closureId);
            if (closure == null) {
                throw new IllegalArgumentException("Cash closure not found");
            }
            for (InventoryAdjustment adjustment : adjustments.findByClosureId(connection, closureId)) {
                if (!adjustment.reason().startsWith("Reversión de ")) {
                    Guide guide = guides.findById(connection, adjustment.guideId());
                    if (guide == null) throw new IllegalArgumentException("No se encontró una guía del cierre");
                    int reverse = Math.negateExact(adjustment.quantityDelta());
                    InventoryAdjustmentTransactionService.applyDelta(guide, reverse);
                    guides.update(connection, guide);
                    adjustments.save(connection, new InventoryAdjustment(
                            0, adjustment.guideId(), reverse, InventoryAdjustmentType.COUNT_CORRECTION,
                            "Reversión de ajuste por cierre anulado: " + reason,
                            java.time.LocalDateTime.now(), closureId
                    ));
                }
            }
            closure.cancel(reason);
            repository.update(connection, closure);
            return null;
        });
    }
}
