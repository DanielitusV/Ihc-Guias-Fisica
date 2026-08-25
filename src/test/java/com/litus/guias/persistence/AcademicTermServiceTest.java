package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderPaymentCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AcademicTermServiceTest {
    @TempDir Path tempDir;
    private Database database;
    private AcademicTermRepository terms;
    private GuideRepository guides;
    private AccountRepository accounts;
    private long guideId;
    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
        terms = new AcademicTermRepository(database);
        guides = new GuideRepository(database);
        accounts = new AccountRepository(database);
        guideId = guides.save(new Guide(0, "Física I", new BigDecimal("35"), 4));
        accountId = accounts.save(new Account(0, "Efectivo", new BigDecimal("80")));
    }

    @Test
    void closingRequiresPasswordAndSecondConfirmation() throws Exception {
        AcademicTermService service = new AcademicTermService(database);

        assertThrows(SecurityException.class, () -> service.closeActive(
                "incorrecta", true, LocalDateTime.of(2026, 12, 20, 10, 0)));
        assertThrows(IllegalArgumentException.class, () -> service.closeActive(
                "malditolitus", false, LocalDateTime.of(2026, 12, 20, 10, 0)));

        assertNotNull(terms.findActive());
        assertEquals(4, guides.findById(guideId).getStock());
        assertEquals(0, new BigDecimal("80").compareTo(accounts.findById(accountId).getBalance()));
    }

    @Test
    void closingArchivesTermAndResetsOperationalBalances() throws Exception {
        AcademicTermService service = new AcademicTermService(database);
        AcademicTerm old = terms.findActive();

        service.closeActive("malditolitus", true,
                LocalDateTime.of(2026, 12, 20, 10, 0));

        assertNull(terms.findActive());
        assertEquals(AcademicTermStatus.CLOSED, terms.findById(old.id()).status());
        assertEquals(0, guides.findById(guideId).getStock());
        assertEquals(0, BigDecimal.ZERO.compareTo(accounts.findById(accountId).getBalance()));
    }

    @Test
    void onlyRegularTermsCanBeOpened() throws Exception {
        AcademicTermService service = new AcademicTermService(database);
        service.closeActive("malditolitus", true, LocalDateTime.of(2026, 12, 20, 10, 0));

        assertThrows(IllegalArgumentException.class,
                () -> service.open("3-2027", LocalDateTime.of(2027, 1, 5, 9, 0)));
        AcademicTerm opened = service.open("1-2027", LocalDateTime.of(2027, 1, 5, 9, 0));

        assertEquals("1-2027", opened.code());
        assertEquals(AcademicTermStatus.OPEN, opened.status());
    }

    @Test
    void restartDoesNotReopenAClosedTerm() throws Exception {
        new AcademicTermService(database).closeActive(
                "malditolitus", true, LocalDateTime.of(2026, 12, 20, 10, 0));

        database.initialize();

        assertNull(terms.findActive());
        assertEquals(1, terms.findAll().size());
    }

    @Test
    void restartNeverCreatesAnotherTermAfterAnyHistoricalTermExists() throws Exception {
        new AcademicTermService(database).closeActive(
                "malditolitus", true, LocalDateTime.of(2026, 12, 20, 10, 0));
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE academic_terms SET code = '2-2025'");
        }

        database.initialize();

        assertNull(terms.findActive());
        assertEquals(1, terms.findAll().size());
    }

    @Test
    void closedTermRemainsHistoryAndNewWritesUseNewTerm() throws Exception {
        SaleTransactionService sales = new SaleTransactionService(database);
        long oldTermId = terms.findActive().id();
        sales.registerSale(guideId, accountId, PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 20, 10, 0));

        AcademicTermService service = new AcademicTermService(database);
        service.closeActive("malditolitus", true, LocalDateTime.of(2026, 12, 20, 10, 0));
        assertThrows(IllegalStateException.class, () -> sales.registerSale(
                guideId, accountId, PaymentMethod.CASH,
                LocalDateTime.of(2026, 12, 21, 10, 0)));

        service.open("1-2027", LocalDateTime.of(2027, 1, 5, 9, 0));
        Guide guide = guides.findById(guideId);
        guide.addStock(2);
        guides.update(guide);
        sales.registerSale(guideId, accountId, PaymentMethod.CASH,
                LocalDateTime.of(2027, 1, 6, 10, 0));

        assertEquals(1, new SaleRepository(database).findAllByTerm(oldTermId).size());
        assertEquals(1, new SaleRepository(database).findAll().size());
    }

    @Test
    void pendingOrderCostBlocksTermClosure() throws Exception {
        new OrderTransactionService(database).registerOrder(new Order(
                0, OrderPaymentCondition.CREDIT, LocalDateTime.now(),
                List.of(new OrderItem(0, 0, guideId, 5, BigDecimal.ZERO))));

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new AcademicTermService(database).closeActive(
                        "malditolitus", true, LocalDateTime.now()));

        assertTrue(error.getMessage().contains("costo pendiente"));
        assertNotNull(terms.findActive());
    }
}
