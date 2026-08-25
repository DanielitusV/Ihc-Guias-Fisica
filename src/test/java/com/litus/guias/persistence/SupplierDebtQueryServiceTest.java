package com.litus.guias.persistence;

import com.litus.guias.account.Account;
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

class SupplierDebtQueryServiceTest {

    @TempDir
    Path tempDir;

    private Database database;

    private long guideId;
    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(
                "jdbc:sqlite:" + tempDir.resolve("test.db")
        );

        database.initialize();

        GuideRepository guideRepository =
                new GuideRepository(database);

        AccountRepository accountRepository =
                new AccountRepository(database);

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
                        new BigDecimal("200.00")
                )
        );
    }

    @Test
    void calculatesCurrentSupplierDebtFromDatabase()
            throws Exception {

        OrderTransactionService orderService =
                new OrderTransactionService(database);

        Order order = new Order(
                0,
                OrderPaymentCondition.CREDIT,
                LocalDateTime.of(
                        2026,
                        8,
                        15,
                        18,
                        0
                ),
                List.of(
                        new OrderItem(
                                0,
                                0,
                                guideId,
                                5,
                                new BigDecimal("20.00")
                        )
                )
        );

        orderService.registerOrder(order);

        SupplierPaymentTransactionService paymentService =
                new SupplierPaymentTransactionService(database);

        paymentService.registerPayment(
                accountId,
                new BigDecimal("30.00"),
                "Pago parcial",
                LocalDateTime.of(
                        2026,
                        8,
                        15,
                        19,
                        0
                )
        );

        SupplierDebtQueryService service =
                new SupplierDebtQueryService(database);

        BigDecimal debt =
                service.calculateCurrentDebt();

        assertEquals(
                0,
                new BigDecimal("70.00")
                        .compareTo(debt)
        );
    }
}