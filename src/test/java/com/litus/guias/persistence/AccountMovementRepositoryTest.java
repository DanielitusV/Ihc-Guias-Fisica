package com.litus.guias.persistence;

import com.litus.guias.account.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AccountMovementRepositoryTest {
    @TempDir
    Path tempDir;

    private Database database;
    private AccountMovementRepository repository;
    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();

        AccountRepository accountRepository = new AccountRepository(database);
        accountId = accountRepository.save(
                new Account (0, "Efectivo", new BigDecimal("100.00"))
        );

        repository = new AccountMovementRepository(database);
    }

    /*
     * Tests start here
     */

    @Test
    public void savesAndFindsMovement() throws Exception {
        LocalDateTime date = LocalDateTime.of(2026, 8, 15, 20, 30);

        AccountMovement movement = new AccountMovement(
                0,
                accountId,
                AccountMovementType.INCOME,
                AccountMovementConcept.SALE,
                new BigDecimal("35.00"),
                null,
                date
        );

        long id = repository.save(movement);

        AccountMovement found = repository.findById(id);

        assertNotNull(found);
        assertEquals(accountId, found.getAccountId());
        assertEquals(AccountMovementType.INCOME, found.getType());
        assertEquals(AccountMovementConcept.SALE, found.getConcept());
        assertEquals(date, found.getCreatedAt());
        assertEquals(
                0,
                new BigDecimal("35.00").compareTo(found.getAmount())
        );
    }
}
