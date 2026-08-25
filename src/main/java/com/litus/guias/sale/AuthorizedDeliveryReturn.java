package com.litus.guias.sale;

import java.time.LocalDateTime;

public record AuthorizedDeliveryReturn(
        long id,
        long deliveryId,
        long guideId,
        int quantity,
        String reason,
        LocalDateTime createdAt
) {
    public AuthorizedDeliveryReturn {
        if (deliveryId <= 0) throw new IllegalArgumentException("La entrega es obligatoria");
        if (guideId <= 0) throw new IllegalArgumentException("La guía es obligatoria");
        if (quantity <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
        if (createdAt == null) throw new IllegalArgumentException("La fecha es obligatoria");
    }
}
