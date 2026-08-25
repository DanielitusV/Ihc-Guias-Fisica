package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.AccountMovementConcept;
import com.litus.guias.inventory.Guide;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        long guideId = new GuideRepository(database).save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 0)
        );
        new OrderTransactionService(database).registerOrder(
                new Order(
                        0,
                        OrderPaymentCondition.CREDIT,
                        LocalDateTime.of(2026, 8, 15, 20, 0),
                        List.of(new OrderItem(
                                0,
                                0,
                                guideId,
                                5,
                                new BigDecimal("20.00")
                        ))
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

    @Test
    void paymentCannotExceedDebtAndRollsBackAccount() throws Exception {
        SupplierPaymentTransactionService service =
                new SupplierPaymentTransactionService(database);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.registerPayment(
                        accountId,
                        new BigDecimal("120.00"),
                        "Pago excesivo",
                        LocalDateTime.of(2026, 8, 15, 21, 30)
                )
        );

        assertEquals(
                0,
                new BigDecimal("100.00").compareTo(
                        accountRepository.findById(accountId).getBalance()
                )
        );
        assertEquals(
                0,
                movementRepository.findByConcept(
                        AccountMovementConcept.SUPPLIER_PAYMENT
                ).size()
        );
    }
}
