package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.AccountMovementConcept;
import com.litus.guias.account.AccountMovementType;
import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RepositoryQueryTest {

    @TempDir
    Path tempDir;

    private Database database;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
    }

    @Test
    void guideAndAccountQueriesReturnPersistedRows() throws Exception {
        GuideRepository guides = new GuideRepository(database);
        AccountRepository accounts = new AccountRepository(database);

        guides.save(new Guide(0, "Física I", new BigDecimal("35.00"), 4));
        guides.save(new Guide(0, "Física II", new BigDecimal("40.00"), 6));
        accounts.save(new Account(0, "Efectivo", BigDecimal.ZERO));
        accounts.save(new Account(0, "QR / Soto", BigDecimal.ZERO));

        assertEquals(2, guides.findAll().size());
        assertEquals(2, accounts.findAll().size());
        assertNotNull(accounts.findByName("QR / Soto"));
    }

    @Test
    void saleAndMovementQueriesRespectDateRangeAndConcept() throws Exception {
        GuideRepository guides = new GuideRepository(database);
        AccountRepository accounts = new AccountRepository(database);
        AccountMovementRepository movements =
                new AccountMovementRepository(database);

        long guideId = guides.save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 4)
        );
        long accountId = accounts.save(
                new Account(0, "Efectivo", BigDecimal.ZERO)
        );

        SaleTransactionService sales = new SaleTransactionService(database);
        sales.registerSale(
                guideId,
                accountId,
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 14, 12, 0)
        );
        sales.registerSale(
                guideId,
                accountId,
                PaymentMethod.CASH,
                LocalDateTime.of(2026, 8, 15, 12, 0)
        );
        movements.save(new AccountMovement(
                0,
                accountId,
                AccountMovementType.INCOME,
                AccountMovementConcept.OTHER,
                BigDecimal.ONE,
                "Ajuste documentado",
                LocalDateTime.of(2026, 8, 15, 13, 0)
        ));

        SaleRepository saleRepository = new SaleRepository(database);
        assertEquals(2, saleRepository.findAll().size());
        assertEquals(
                1,
                saleRepository.findBetween(
                        LocalDateTime.of(2026, 8, 15, 0, 0),
                        LocalDateTime.of(2026, 8, 16, 0, 0)
                ).size()
        );
        assertEquals(
                1,
                movements.findByConcept(AccountMovementConcept.OTHER).size()
        );
        assertEquals(3, movements.findByAccountId(accountId).size());
        assertEquals(
                2,
                movements.findBetween(
                        LocalDateTime.of(2026, 8, 15, 0, 0),
                        LocalDateTime.of(2026, 8, 16, 0, 0)
                ).size()
        );
    }
}
