package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.CashClosureItem;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.closure.DayStatus;
import com.litus.guias.inventory.Guide;
import com.litus.guias.inventory.InventoryAdjustmentType;
import com.litus.guias.sale.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DayStatusQueryServiceTest {

    @TempDir
    Path tempDir;

    private Database database;
    private DayStatusQueryService service;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
        service = new DayStatusQueryService(database);
    }

    @Test
    void derivesMissedAndNoActivityFromDatabase() throws Exception {
        assertEquals(
                DayStatus.NO_ACTIVITY,
                service.getStatus(
                        LocalDate.of(2026, 8, 14),
                        LocalDate.of(2026, 8, 15)
                )
        );

        long guideId = new GuideRepository(database).save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 2)
        );
        long accountId = new AccountRepository(database).save(
                new Account(0, "Efectivo", BigDecimal.ZERO)
        );
        new SaleTransactionService(database).registerSale(
                guideId,
                accountId,
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 10, 0)
        );

        assertEquals(
                DayStatus.MISSED,
                service.getStatus(
                        LocalDate.of(2026, 8, 14),
                        LocalDate.of(2026, 8, 15)
                )
        );
        assertEquals(
                DayStatus.OPEN,
                service.getStatus(
                        LocalDate.of(2026, 8, 15),
                        LocalDate.of(2026, 8, 15)
                )
        );
    }

    @Test
    void validClosureIsClosedAndCancellationReturnsMissed() throws Exception {
        long guideId = new GuideRepository(database).save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 2)
        );
        CashClosureTransactionService closures =
                new CashClosureTransactionService(database);
        long closureId = closures.registerClosure(
                new CashClosure(
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "Cierre",
                        LocalDateTime.of(2026, 8, 14, 20, 0)
                ),
                List.of(new CashClosureItem(0, 0, guideId, 2, 2))
        );

        assertEquals(
                DayStatus.CLOSED,
                service.getStatus(
                        LocalDate.of(2026, 8, 14),
                        LocalDate.of(2026, 8, 15)
                )
        );

        closures.cancelClosure(closureId, "Cierre prematuro");

        assertEquals(
                DayStatus.MISSED,
                service.getStatus(
                        LocalDate.of(2026, 8, 14),
                        LocalDate.of(2026, 8, 15)
                )
        );
    }

    @Test
    void authorizedDeliveryRequiresDailyClosure() throws Exception {
        long guideId = new GuideRepository(database).save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 2));
        new AuthorizedDeliveryTransactionService(database).register(
                guideId, 1, "Beneficiario", "Responsable", "Apoyo",
                LocalDateTime.of(2026, 8, 14, 10, 0));

        assertEquals(DayStatus.MISSED, service.getStatus(
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 15)));
    }

    @Test
    void authorizedDeliveryReturnRequiresDailyClosure() throws Exception {
        long guideId = new GuideRepository(database).save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 2));
        long deliveryId = new AuthorizedDeliveryTransactionService(database).register(
                guideId, 1, "Beneficiario", "Responsable", "Préstamo",
                LocalDateTime.of(2026, 8, 13, 10, 0));
        new AuthorizedDeliveryReturnTransactionService(database).register(
                deliveryId, 1, "Devuelta",
                LocalDateTime.of(2026, 8, 14, 10, 0));

        assertEquals(DayStatus.MISSED, service.getStatus(
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 15)));
    }

    @Test
    void inventoryAdjustmentRequiresDailyClosure() throws Exception {
        long guideId = new GuideRepository(database).save(
                new Guide(0, "Física II", new BigDecimal("35.00"), 0));
        new InventoryAdjustmentTransactionService(database).register(
                guideId, 20, InventoryAdjustmentType.CARRYOVER,
                "Ingreso gratuito", LocalDateTime.of(2026, 8, 14, 10, 0));

        assertEquals(DayStatus.MISSED, service.getStatus(
                LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 15)));
    }

    @Test
    void listsOnlyPastDaysWithActivityAndWithoutValidClosure() throws Exception {
        try (var connection = database.getConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE academic_terms SET opened_at = '2026-08-13T08:00'");
        }
        long guideId = new GuideRepository(database).save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 3)
        );
        long accountId = new AccountRepository(database).save(
                new Account(0, "Efectivo", BigDecimal.ZERO)
        );
        new SaleTransactionService(database).registerSale(
                guideId, accountId, PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 10, 0)
        );

        assertEquals(List.of(LocalDate.of(2026, 8, 14)),
                service.findMissedDays(LocalDate.of(2026, 8, 16)));

        new CashClosureTransactionService(database).registerClosure(
                new CashClosure(
                        0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        "Cierre recuperado", LocalDate.of(2026, 8, 14),
                        LocalDateTime.of(2026, 8, 16, 9, 0)
                ),
                List.of(new CashClosureItem(0, 0, guideId, 2, 2))
        );

        assertEquals(List.of(), service.findMissedDays(LocalDate.of(2026, 8, 16)));
    }
}
