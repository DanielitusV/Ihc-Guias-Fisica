package com.litus.guias.ui;

import com.litus.guias.account.Account;
import com.litus.guias.inventory.Guide;
import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderPaymentCondition;
import com.litus.guias.persistence.AccountRepository;
import com.litus.guias.persistence.ApplicationBootstrap;
import com.litus.guias.persistence.Database;
import com.litus.guias.persistence.GuideRepository;
import com.litus.guias.persistence.OrderTransactionService;
import com.litus.guias.persistence.SaleTransactionService;
import com.litus.guias.sale.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppQueriesTest {

    @TempDir
    Path tempDir;

    @Test
    void todaySummaryCountsOnlyActiveSalesAndReturnsCurrentBalances()
            throws Exception {
        ApplicationBootstrap bootstrap = new ApplicationBootstrap(tempDir);
        bootstrap.initializeGuides(Map.of(
                "Física General", new BigDecimal("30"),
                "Física I", new BigDecimal("31"),
                "Física II", new BigDecimal("32"),
                "Física III", new BigDecimal("33")
        ));
        Database database = bootstrap.initialize();
        Guide guide = new GuideRepository(database).findAll().getFirst();
        Account cash = new AccountRepository(database).findByName("Efectivo");
        new OrderTransactionService(database).registerOrder(new Order(
                0,
                OrderPaymentCondition.PAID,
                LocalDateTime.of(2026, 8, 16, 8, 0),
                List.of(new OrderItem(0, 0, guide.getId(), 2, new BigDecimal("10")))
        ));
        SaleTransactionService sales = new SaleTransactionService(database);
        sales.registerSale(
                guide.getId(), cash.getId(), PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 16, 9, 0)
        );
        long cancelled = sales.registerSale(
                guide.getId(), cash.getId(), PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 16, 10, 0)
        );
        sales.cancelSale(cancelled, "Error de registro", LocalDateTime.of(2026, 8, 16, 10, 1));

        AppQueries.TodaySummary summary = new AppQueries(database)
                .todaySummary(LocalDate.of(2026, 8, 16));

        assertEquals(1, summary.activeSales());
        assertEquals(0, summary.income().compareTo(guide.getCurrentPrice()));
        assertEquals(0, summary.cashBalance().compareTo(guide.getCurrentPrice()));
        assertEquals(0, summary.qrBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.supplierDebt().compareTo(BigDecimal.ZERO));
    }

    @Test
    void weeklyGuideSummaryUsesMondayBoundariesAndIgnoresCancelledSales()
            throws Exception {
        ApplicationBootstrap bootstrap = new ApplicationBootstrap(tempDir);
        bootstrap.initializeGuides(Map.of(
                "Física General", new BigDecimal("30"),
                "Física I", new BigDecimal("31"),
                "Física II", new BigDecimal("32"),
                "Física III", new BigDecimal("33")
        ));
        Database database = bootstrap.initialize();
        Guide guide = new GuideRepository(database).findAll().stream()
                .filter(item -> item.getName().equals("Física General"))
                .findFirst().orElseThrow();
        Account cash = new AccountRepository(database).findByName("Efectivo");
        new OrderTransactionService(database).registerOrder(new Order(
                0,
                OrderPaymentCondition.PAID,
                LocalDateTime.of(2026, 8, 9, 8, 0),
                List.of(new OrderItem(0, 0, guide.getId(), 8, new BigDecimal("10")))
        ));
        SaleTransactionService sales = new SaleTransactionService(database);
        sales.registerSale(guide.getId(), cash.getId(), PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 10, 9, 0));
        sales.registerSale(guide.getId(), cash.getId(), PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 16, 18, 0));
        sales.registerSale(guide.getId(), cash.getId(), PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 17, 8, 0));
        long cancelled = sales.registerSale(guide.getId(), cash.getId(), PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 18, 9, 0));
        sales.cancelSale(cancelled, "Venta duplicada", LocalDateTime.of(2026, 8, 18, 9, 1));
        sales.registerSale(guide.getId(), cash.getId(), PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 24, 8, 0));

        AppQueries.GuideWeekSummary result = new AppQueries(database)
                .weeklyGuideSummary(LocalDate.of(2026, 8, 19)).stream()
                .filter(item -> item.guideName().equals("Física General"))
                .findFirst().orElseThrow();

        assertEquals(1, result.currentWeek());
        assertEquals(2, result.previousWeek());
    }
}
