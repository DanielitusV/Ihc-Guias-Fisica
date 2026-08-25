package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizedDeliveryTransactionServiceTest {
    @TempDir Path tempDir;
    private Database database;
    private GuideRepository guides;
    private long guideId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
        guides = new GuideRepository(database);
        guideId = guides.save(new Guide(0, "Física II", new BigDecimal("35"), 3));
    }

    @Test
    void registersAuthorizedDeliveryWithoutIncome() throws Exception {
        new AuthorizedDeliveryTransactionService(database).register(
                guideId, 2, "Estudiante invitado", "Presidenta CEF",
                "Apoyo autorizado", LocalDateTime.of(2026, 8, 18, 10, 0));

        assertEquals(1, guides.findById(guideId).getStock());
        assertEquals(1, new AuthorizedDeliveryRepository(database).findAll().size());
        long termId = new AcademicTermRepository(database).findActive().id();
        assertEquals(1, new AuthorizedDeliveryRepository(database).findBetweenByTerm(
                termId, LocalDateTime.of(2026, 8, 18, 0, 0),
                LocalDateTime.of(2026, 8, 19, 0, 0)).size());
        assertEquals(0, new AccountMovementRepository(database).findAll().size());
    }

    @Test
    void rejectsInsufficientStockWithoutPartialChanges() {
        assertThrows(IllegalStateException.class, () ->
                new AuthorizedDeliveryTransactionService(database).register(
                        guideId, 4, "Beneficiario", "Responsable",
                        "Motivo", LocalDateTime.now()));
        try {
            assertEquals(3, guides.findById(guideId).getStock());
            assertEquals(0, new AuthorizedDeliveryRepository(database).findAll().size());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    @Test
    void returnsAuthorizedDeliveryWithoutMovingMoney() throws Exception {
        long deliveryId = new AuthorizedDeliveryTransactionService(database).register(
                guideId, 2, "Beneficiario", "Responsable", "Préstamo",
                LocalDateTime.of(2026, 8, 18, 10, 0));

        new AuthorizedDeliveryReturnTransactionService(database).register(
                deliveryId, 1, "Devolución parcial",
                LocalDateTime.of(2026, 8, 18, 15, 0));

        assertEquals(2, guides.findById(guideId).getStock());
        assertEquals(1, new AuthorizedDeliveryReturnRepository(database).findAll().size());
        assertEquals(0, new AccountMovementRepository(database).findAll().size());
    }

    @Test
    void rejectsReturningMoreThanPendingWithoutPartialChanges() throws Exception {
        long deliveryId = new AuthorizedDeliveryTransactionService(database).register(
                guideId, 2, "Beneficiario", "Responsable", "Préstamo",
                LocalDateTime.of(2026, 8, 18, 10, 0));

        assertThrows(IllegalArgumentException.class, () ->
                new AuthorizedDeliveryReturnTransactionService(database).register(
                        deliveryId, 3, "Cantidad incorrecta",
                        LocalDateTime.of(2026, 8, 18, 15, 0)));

        assertEquals(1, guides.findById(guideId).getStock());
        assertEquals(0, new AuthorizedDeliveryReturnRepository(database).findAll().size());
    }
}
