package com.litus.guias.ui;

import com.litus.guias.account.Account;
import com.litus.guias.inventory.Guide;
import com.litus.guias.persistence.AccountRepository;
import com.litus.guias.persistence.ApplicationBootstrap;
import com.litus.guias.persistence.Database;
import com.litus.guias.persistence.GuideRepository;
import com.litus.guias.persistence.SaleRepository;
import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.SaleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuickSaleServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void selectsCanonicalAccountAndCanUndoLastQuickSale() throws Exception {
        ApplicationBootstrap bootstrap = new ApplicationBootstrap(tempDir);
        bootstrap.initializeGuides(Map.of(
                "Física General", new BigDecimal("30"),
                "Física I", new BigDecimal("31"),
                "Física II", new BigDecimal("32"),
                "Física III", new BigDecimal("33")
        ));
        Database database = bootstrap.initialize();
        GuideRepository guides = new GuideRepository(database);
        Guide guide = guides.findAll().getFirst();
        guide.addStock(2);
        guides.update(guide);

        QuickSaleService service = new QuickSaleService(database);
        long cashSale = service.register(
                guide.getId(), PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 16, 9, 0)
        );
        long qrSale = service.register(
                guide.getId(), PaymentMethod.QR,
                LocalDateTime.of(2026, 8, 16, 9, 1)
        );

        AccountRepository accounts = new AccountRepository(database);
        Account cash = accounts.findByName("Efectivo");
        Account qr = accounts.findByName("QR / Soto");
        SaleRepository sales = new SaleRepository(database);

        assertEquals(cash.getId(), sales.findById(cashSale).getAccountId());
        assertEquals(qr.getId(), sales.findById(qrSale).getAccountId());
        assertEquals(0, guide.getCurrentPrice().compareTo(cash.getBalance()));
        assertEquals(0, guide.getCurrentPrice().compareTo(qr.getBalance()));
        assertEquals(0, guides.findById(guide.getId()).getStock());

        service.undo(qrSale, LocalDateTime.of(2026, 8, 16, 9, 2));

        assertEquals(SaleStatus.CANCELLED, sales.findById(qrSale).getStatus());
        assertEquals(1, guides.findById(guide.getId()).getStock());
        assertEquals(0, BigDecimal.ZERO.compareTo(
                accounts.findByName("QR / Soto").getBalance()
        ));
    }

    @Test
    void guidesUseTheSameCanonicalOrderAsTheExcelDashboard() throws Exception {
        Database database = new Database("jdbc:sqlite:" + tempDir.resolve("order.db"));
        database.initialize();
        GuideRepository repository = new GuideRepository(database);
        repository.save(new Guide(0, "Física II", new BigDecimal("32"), 0));
        repository.save(new Guide(0, "Física General", new BigDecimal("30"), 0));
        repository.save(new Guide(0, "Física III", new BigDecimal("33"), 0));
        repository.save(new Guide(0, "Física I", new BigDecimal("31"), 0));

        assertEquals(
                java.util.List.of("Física General", "Física I", "Física II", "Física III"),
                repository.findAll().stream()
                        .map(Guide::getName)
                        .toList()
        );
    }
}
