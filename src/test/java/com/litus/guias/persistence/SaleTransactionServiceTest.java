package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.CashClosureItem;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.SaleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SaleTransactionServiceTest {

    @TempDir
    Path tempDir;

    private Database database;
    private GuideRepository guideRepository;
    private AccountRepository accountRepository;
    private SaleRepository saleRepository;

    private long guideId;
    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(
                "jdbc:sqlite:" + tempDir.resolve("test.db")
        );
        database.initialize();

        guideRepository = new GuideRepository(database);
        accountRepository = new AccountRepository(database);
        saleRepository = new SaleRepository(database);

        guideId = guideRepository.save(
                new Guide(
                        0,
                        "Física I",
                        new BigDecimal("35.00"),
                        10
                )
        );

        accountId = accountRepository.save(
                new Account(
                        0,
                        "Efectivo",
                        new BigDecimal("100.00")
                )
        );
    }

    @Test
    void registersCompleteSale() throws Exception {
        SaleTransactionService service =
                new SaleTransactionService(database);

        LocalDateTime date =
                LocalDateTime.of(2026, 8, 15, 21, 0);

        long saleId = service.registerSale(
                guideId,
                accountId,
                PaymentMethod.CASH,
                date
        );

        Guide guide = guideRepository.findById(guideId);
        Account account = accountRepository.findById(accountId);
        Sale sale = saleRepository.findById(saleId);

        assertEquals(9, guide.getStock());

        assertEquals(
                0,
                new BigDecimal("135.00")
                        .compareTo(account.getBalance())
        );

        assertNotNull(sale);
        assertEquals(guideId, sale.getGuideId());

        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(
                     """
                     SELECT COUNT(*)
                     FROM account_movements
                     WHERE account_id = ?
                     AND concept = 'SALE'
                     """
             )) {

            statement.setLong(1, accountId);

            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }

    @Test
    void cancelsCompleteSale() throws Exception {
        SaleTransactionService service =
                new SaleTransactionService(database);

        long saleId = service.registerSale(
                guideId,
                accountId,
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 15, 20, 0)
        );

        service.cancelSale(
                saleId,
                accountId,
                "Venta registrada por error",
                LocalDateTime.of(2026, 8, 15, 20, 5)
        );

        Guide guide = guideRepository.findById(guideId);
        Account account = accountRepository.findById(accountId);
        Sale sale = saleRepository.findById(saleId);

        assertEquals(10, guide.getStock());

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(account.getBalance())
        );

        assertEquals(
                SaleStatus.CANCELLED,
                sale.getStatus()
        );

        assertEquals(
                "Venta registrada por error",
                sale.getCancellationReason()
        );

        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(
                     """
                     SELECT COUNT(*)
                     FROM account_movements
                     WHERE account_id = ?
                     AND concept = 'SALE_CANCELLATION'
                     """
             )) {

            statement.setLong(1, accountId);

            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }

    @Test
    void blocksSaleAfterValidClosure() throws Exception {
        CashClosureRepository closureRepository =
                new CashClosureRepository(database);

        CashClosure closure = new CashClosure(
                0,
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Cierre del día",
                LocalDateTime.of(2026, 8, 15, 21, 0)
        );

        closureRepository.save(
                closure,
                List.of(
                        new CashClosureItem(
                                0,
                                0,
                                guideId,
                                10,
                                10
                        )
                )
        );

        SaleTransactionService service =
                new SaleTransactionService(database);

        assertThrows(
                IllegalStateException.class,
                () -> service.registerSale(
                        guideId,
                        accountId,
                        PaymentMethod.CASH,
                        LocalDateTime.of(
                                2026,
                                8,
                                15,
                                21,
                                5
                        )
                )
        );

        Guide guide = guideRepository.findById(guideId);
        Account account = accountRepository.findById(accountId);

        assertEquals(10, guide.getStock());

        assertEquals(
                0,
                new BigDecimal("100.00")
                        .compareTo(account.getBalance())
        );
    }

    @Test
    void cancellationUsesAccountStoredBySale() throws Exception {
        long otherAccountId = accountRepository.save(
                new Account(0, "QR / Soto", new BigDecimal("200.00"))
        );

        SaleTransactionService service =
                new SaleTransactionService(database);

        long saleId = service.registerSale(
                guideId,
                accountId,
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 15, 18, 0)
        );

        service.cancelSale(
                saleId,
                "Error de registro",
                LocalDateTime.of(2026, 8, 15, 18, 5)
        );

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(
                        accountRepository.findById(accountId).getBalance()
                )
        );
        assertEquals(
                0,
                new BigDecimal("200.00").compareTo(
                        accountRepository.findById(otherAccountId).getBalance()
                )
        );
        assertEquals(
                accountId,
                saleRepository.findById(saleId).getAccountId()
        );
    }
}
