package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.AuthorizedDelivery;

import java.time.LocalDateTime;

public final class AuthorizedDeliveryTransactionService {
    private final Database database;
    private final GuideRepository guides;
    private final AuthorizedDeliveryRepository deliveries;
    private final CashClosureRepository closures;

    public AuthorizedDeliveryTransactionService(Database database) {
        this.database = database;
        guides = new GuideRepository(database);
        deliveries = new AuthorizedDeliveryRepository(database);
        closures = new CashClosureRepository(database);
    }

    public long register(long guideId, int quantity, String beneficiary,
            String authorizedBy, String reason, LocalDateTime createdAt) throws Exception {
        AuthorizedDelivery delivery = new AuthorizedDelivery(
                0, guideId, quantity, beneficiary, authorizedBy, reason, createdAt);
        return database.inTransaction(connection -> {
            if (closures.findValidByDate(connection, createdAt.toLocalDate()) != null) {
                throw new IllegalStateException("No se puede registrar una entrega en una jornada cerrada");
            }
            Guide guide = guides.findById(connection, guideId);
            if (guide == null) throw new IllegalArgumentException("Guía no encontrada");
            guide.removeStock(quantity);
            guides.update(connection, guide);
            return deliveries.save(connection, delivery);
        });
    }
}
