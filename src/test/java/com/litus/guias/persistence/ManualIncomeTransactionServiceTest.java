package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovementConcept;
import com.litus.guias.account.AccountMovementType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ManualIncomeTransactionServiceTest {
    @TempDir Path tempDir;

    @Test
    void registersIncomeWithRequiredReasonInActiveTerm() throws Exception {
        Database database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
        AccountRepository accounts = new AccountRepository(database);
        long accountId = accounts.save(new Account(0, "Efectivo", BigDecimal.ZERO));
        ManualIncomeTransactionService service = new ManualIncomeTransactionService(database);

        long movementId = service.register(
                accountId, new BigDecimal("125.50"), "Sobrante gestión anterior",
                LocalDateTime.of(2027, 1, 6, 9, 0));

        assertEquals(0, new BigDecimal("125.50").compareTo(
                accounts.findById(accountId).getBalance()));
        var movement = new AccountMovementRepository(database).findById(movementId);
        assertEquals(AccountMovementType.INCOME, movement.getType());
        assertEquals(AccountMovementConcept.OTHER, movement.getConcept());
        assertEquals("Sobrante gestión anterior", movement.getReason());
    }

    @Test
    void rejectsBlankReason() throws Exception {
        Database database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
        AccountRepository accounts = new AccountRepository(database);
        long accountId = accounts.save(new Account(0, "Efectivo", BigDecimal.ZERO));

        assertThrows(IllegalArgumentException.class, () ->
                new ManualIncomeTransactionService(database).register(
                        accountId, BigDecimal.TEN, " ", LocalDateTime.now()));
        assertEquals(0, BigDecimal.ZERO.compareTo(accounts.findById(accountId).getBalance()));
    }
}
