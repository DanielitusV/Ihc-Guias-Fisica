package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class SaleRepositoryTest {
    @TempDir
    Path tempDir;

    private Database database;
    private SaleRepository repository;
    private long guideId;
    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();

        GuideRepository guideRepository = new GuideRepository(database);
        guideId = guideRepository.save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 10)
        );
        accountId = new AccountRepository(database).save(
                new com.litus.guias.account.Account(
                        0,
                        "Efectivo",
                        BigDecimal.ZERO
                )
        );
        repository = new SaleRepository(database);

        /*
         * Tests starts here
         */
    }

    @Test
    public void savesAndFindsSale() throws Exception {
        LocalDateTime date = LocalDateTime.of(2026, 8, 15, 14, 30);

        Sale sale = new Sale(
                0,
                guideId,
                accountId,
                new BigDecimal("35.00"),
                PaymentMethod.CASH,
                date,
                SaleStatus.ACTIVE
        );

        long id = repository.save(sale);
        Sale found = repository.findById(id);

        assertNotNull(found);
        assertEquals(id, found.getGuideId());
        assertEquals(guideId, found.getGuideId());
        assertEquals(PaymentMethod.CASH, found.getPaymentMethod());
        assertEquals(SaleStatus.ACTIVE, found.getStatus());
        assertEquals(date, found.getCreatedAt());
        assertEquals(0, new BigDecimal("35.00").compareTo(found.getPrice()));
    }
}
