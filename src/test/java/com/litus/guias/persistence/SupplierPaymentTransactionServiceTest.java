package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.AccountMovementConcept;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupplierPaymentTransactionServiceTest {

    @TempDir
    Path tempDir;

    private Database database;
    private AccountRepository accountRepository;
    private AccountMovementRepository movementRepository;
    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(
                "jdbc:sqlite:" + tempDir.resolve("test.db")
        );
        database.initialize();

        accountRepository = new AccountRepository(database);
        movementRepository = new AccountMovementRepository(database);

        accountId = accountRepository.save(
                new Account(
                        0,
                        "Efectivo",
                        new BigDecimal("100.00")
                )
        );
    }

    @Test
    void registersSupplierPayment() throws Exception {
        SupplierPaymentTransactionService service =
                new SupplierPaymentTransactionService(database);

        long movementId = service.registerPayment(
                accountId,
                new BigDecimal("30.00"),
                "Pago parcial a fotocopiadora",
                LocalDateTime.of(2026, 8, 15, 21, 30)
        );

        Account account =
                accountRepository.findById(accountId);

        AccountMovement movement =
                movementRepository.findById(movementId);

        assertEquals(
                0,
                new BigDecimal("70.00")
                        .compareTo(account.getBalance())
        );

        assertEquals(
                AccountMovementConcept.SUPPLIER_PAYMENT,
                movement.getConcept()
        );

        assertEquals(
                "Pago parcial a fotocopiadora",
                movement.getReason()
        );
    }
}
