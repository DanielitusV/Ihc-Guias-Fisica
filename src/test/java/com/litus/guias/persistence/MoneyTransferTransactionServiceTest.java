package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovementConcept;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTransferTransactionServiceTest {
    @TempDir Path tempDir;
    private Database database;
    private AccountRepository accounts;
    private long cashId;
    private long managerId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database("jdbc:sqlite:" + tempDir.resolve("test.db"));
        database.initialize();
        accounts = new AccountRepository(database);
        cashId = accounts.save(new Account(0, "Efectivo", new BigDecimal("2000")));
        managerId = accounts.save(new Account(0, "Cuenta del encargado", BigDecimal.ZERO));
    }

    @Test
    void transfersMoneyAtomicallyWithoutCreatingIncome() throws Exception {
        new MoneyTransferTransactionService(database).transfer(
                cashId, managerId, new BigDecimal("1000"),
                "Fondos para gastos operativos", LocalDateTime.of(2026, 8, 18, 9, 0));

        assertEquals(0, accounts.findById(cashId).getBalance().compareTo(new BigDecimal("1000")));
        assertEquals(0, accounts.findById(managerId).getBalance().compareTo(new BigDecimal("1000")));
        assertEquals(2, new AccountMovementRepository(database)
                .findByConcept(AccountMovementConcept.TRANSFER).size());
    }

    @Test
    void rejectsSameAccountAndInsufficientBalance() {
        MoneyTransferTransactionService service = new MoneyTransferTransactionService(database);
        assertThrows(IllegalArgumentException.class, () -> service.transfer(
                cashId, cashId, BigDecimal.ONE, "Inválida", LocalDateTime.now()));
        assertThrows(IllegalStateException.class, () -> service.transfer(
                cashId, managerId, new BigDecimal("3000"), "Sin saldo", LocalDateTime.now()));
    }
}
