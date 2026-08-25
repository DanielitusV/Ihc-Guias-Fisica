package com.litus.guias.sale;

import java.time.LocalDateTime;

public record AuthorizedDelivery(
        long id,
        long guideId,
        int quantity,
        String beneficiary,
        String authorizedBy,
        String reason,
        LocalDateTime createdAt
) {
    public AuthorizedDelivery {
        if (quantity <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        if (beneficiary == null || beneficiary.isBlank()) throw new IllegalArgumentException("El beneficiario es obligatorio");
        if (authorizedBy == null || authorizedBy.isBlank()) throw new IllegalArgumentException("La persona que autoriza es obligatoria");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
        if (createdAt == null) throw new IllegalArgumentException("La fecha es obligatoria");
    }
}
