package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.AuthorizedDelivery;
import com.litus.guias.sale.AuthorizedDeliveryReturn;

import java.time.LocalDateTime;

public final class AuthorizedDeliveryReturnTransactionService {
    private final Database database;
    private final GuideRepository guides;
    private final AuthorizedDeliveryRepository deliveries;
    private final AuthorizedDeliveryReturnRepository returns;
    private final CashClosureRepository closures;

    public AuthorizedDeliveryReturnTransactionService(Database database) {
        this.database = database;
        guides = new GuideRepository(database);
        deliveries = new AuthorizedDeliveryRepository(database);
        returns = new AuthorizedDeliveryReturnRepository(database);
        closures = new CashClosureRepository(database);
    }

    public long register(long deliveryId, int quantity, String reason, LocalDateTime createdAt)
            throws Exception {
        if (quantity <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
        if (createdAt == null) throw new IllegalArgumentException("La fecha es obligatoria");
        return database.inTransaction(connection -> {
            if (closures.findValidByDate(connection, createdAt.toLocalDate()) != null) {
                throw new IllegalStateException("No se puede registrar una devolución en una jornada cerrada");
            }
            AuthorizedDelivery delivery = deliveries.findById(connection, deliveryId);
            if (delivery == null) throw new IllegalArgumentException("Entrega autorizada no encontrada");
            int pending = delivery.quantity() - returns.returnedQuantity(connection, deliveryId);
            if (quantity > pending) {
                throw new IllegalArgumentException("Solo quedan " + pending + " unidades pendientes de devolución");
            }
            Guide guide = guides.findById(connection, delivery.guideId());
            if (guide == null) throw new IllegalArgumentException("Guía no encontrada");
            guide.addStock(quantity);
            guides.update(connection, guide);
            return returns.save(connection, new AuthorizedDeliveryReturn(
                    0, deliveryId, delivery.guideId(), quantity, reason, createdAt));
        });
    }
}
