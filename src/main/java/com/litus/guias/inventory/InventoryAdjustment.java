package com.litus.guias.inventory;

import java.time.LocalDateTime;

public record InventoryAdjustment(
        long id,
        long guideId,
        int quantityDelta,
        InventoryAdjustmentType type,
        String reason,
        LocalDateTime createdAt,
        Long cashClosureId
) {
    public InventoryAdjustment(long id, long guideId, int quantityDelta,
            InventoryAdjustmentType type, String reason, LocalDateTime createdAt) {
        this(id, guideId, quantityDelta, type, reason, createdAt, null);
    }

    public InventoryAdjustment {
        if (guideId <= 0) throw new IllegalArgumentException("Guide is required");
        if (quantityDelta == 0) throw new IllegalArgumentException("Adjustment quantity cannot be zero");
        if (type == null) throw new IllegalArgumentException("Adjustment type is required");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Adjustment reason is required");
        if (createdAt == null) throw new IllegalArgumentException("Adjustment date is required");
        reason = reason.trim();
    }
}
