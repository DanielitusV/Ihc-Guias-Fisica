package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AccountRepositoryTest {
    @TempDir
    Path tempDir;

    private Database database;
    private AccountRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();

        repository = new AccountRepository(database);
    }

    /*
     * Tests starts here
     */

    @Test
    public void savesAndFindsAccount() throws Exception {
        Account account = new Account(0, "Efectivo", new BigDecimal("100.00"));

        long id = repository.save(account);
        Account found = repository.findById(id);
        assertNotNull(found);
        assertEquals(id, found.getId());
        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(found.getBalance())
        );
    }

    @Test
    public void updatesAccount() throws Exception {
        long id = repository.save(
                new Account(0, "Efectivo", new BigDecimal("100.00"))
        );

        Account account = repository.findById(id);
        account.addExpense(new BigDecimal("25.00"));
        repository.update(account);
        Account updated = repository.findById(id);

        assertEquals(
                0,
                new BigDecimal("75.00").compareTo(updated.getBalance())
        );
    }
}
