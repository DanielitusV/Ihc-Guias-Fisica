package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.inventory.Guide;
import com.litus.guias.inventory.InventoryAdjustmentType;
import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderPaymentCondition;
import com.litus.guias.sale.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClosureSnapshotQueryServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void reconstructsBalancesAndStockAtEndOfSelectedDay() throws Exception {
        ApplicationBootstrap bootstrap = new ApplicationBootstrap(tempDir);
        bootstrap.initializeGuides(java.util.Map.of(
                "Física General", new BigDecimal("35"),
                "Física I", new BigDecimal("35"),
                "Física II", new BigDecimal("35"),
                "Física III", new BigDecimal("35")
        ));
        Database database = bootstrap.initialize();
        Guide guide = new GuideRepository(database).findAll().stream()
                .filter(value -> value.getName().equals("Física I"))
                .findFirst().orElseThrow();
        Account cash = new AccountRepository(database).findByName("Efectivo");
        Account qr = new AccountRepository(database).findByName("QR / Soto");
        Account manager = new AccountRepository(database).findByName("Cuenta del encargado");
        new OrderTransactionService(database).registerOrder(new Order(
                0, OrderPaymentCondition.CREDIT, LocalDateTime.of(2026, 8, 14, 9, 0),
                List.of(new OrderItem(0, 0, guide.getId(), 3, new BigDecimal("20")))
        ));
        new InventoryAdjustmentTransactionService(database).register(
                guide.getId(), 2, InventoryAdjustmentType.OMITTED_STOCK,
                "Sobrante encontrado", LocalDateTime.of(2026, 8, 15, 8, 0));
        SaleTransactionService sales = new SaleTransactionService(database);
        long firstSale = sales.registerSale(
                guide.getId(), cash.getId(), PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 15, 10, 0)
        );
        new MoneyTransferTransactionService(database).transfer(
                cash.getId(), manager.getId(), new BigDecimal("20"),
                "Gastos operativos", LocalDateTime.of(2026, 8, 15, 11, 0));
        long deliveryId = new AuthorizedDeliveryTransactionService(database).register(
                guide.getId(), 1, "Beneficiario", "Responsable", "Apoyo",
                LocalDateTime.of(2026, 8, 15, 12, 0));
        sales.registerSale(
                guide.getId(), qr.getId(), PaymentMethod.QR,
                LocalDateTime.of(2026, 8, 16, 10, 0)
        );
        new AuthorizedDeliveryReturnTransactionService(database).register(
                deliveryId, 1, "Guía devuelta",
                LocalDateTime.of(2026, 8, 16, 12, 0));
        sales.cancelSale(firstSale, "Anulada después", LocalDateTime.of(2026, 8, 17, 8, 0));

        ClosureSnapshotQueryService.Snapshot snapshot =
                new ClosureSnapshotQueryService(database).atEndOf(LocalDate.of(2026, 8, 15));

        assertEquals(0, snapshot.expectedCash().compareTo(new BigDecimal("15")));
        assertEquals(0, snapshot.expectedQr().compareTo(BigDecimal.ZERO));
        assertEquals(3, snapshot.expectedStock().get(guide.getId()));

        ClosureSnapshotQueryService.Snapshot nextDay =
                new ClosureSnapshotQueryService(database).atEndOf(LocalDate.of(2026, 8, 16));
        assertEquals(3, nextDay.expectedStock().get(guide.getId()));
    }
}
