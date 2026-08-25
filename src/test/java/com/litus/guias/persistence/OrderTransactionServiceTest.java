package com.litus.guias.persistence;

import com.litus.guias.inventory.Guide;
import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderPaymentCondition;
import com.litus.guias.order.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTransactionServiceTest {

    @TempDir
    Path tempDir;

    private Database database;
    private GuideRepository guideRepository;

    private long guideId1;
    private long guideId2;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(
                "jdbc:sqlite:" + tempDir.resolve("test.db")
        );
        database.initialize();

        guideRepository = new GuideRepository(database);

        guideId1 = guideRepository.save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 10)
        );

        guideId2 = guideRepository.save(
                new Guide(0, "Física II", new BigDecimal("40.00"), 20)
        );
    }

    @Test
    void registersCompleteOrder() throws Exception {
        OrderTransactionService service =
                new OrderTransactionService(database);

        Order order = new Order(
                0,
                OrderPaymentCondition.CREDIT,
                LocalDateTime.of(2026, 8, 15, 21, 30),
                List.of(
                        new OrderItem(
                                0, 0, guideId1,
                                5, new BigDecimal("20.00")
                        ),
                        new OrderItem(
                                0, 0, guideId2,
                                3, new BigDecimal("25.00")
                        )
                )
        );

        long orderId = service.registerOrder(order);

        Guide guide1 = guideRepository.findById(guideId1);
        Guide guide2 = guideRepository.findById(guideId2);

        OrderRepository orderRepository =
                new OrderRepository(database);

        Order savedOrder = orderRepository.findById(orderId);

        assertEquals(15, guide1.getStock());
        assertEquals(23, guide2.getStock());

        assertNotNull(savedOrder);
        assertEquals(
                OrderPaymentCondition.CREDIT,
                savedOrder.getPaymentCondition()
        );
        assertEquals(2, savedOrder.getItems().size());
    }

    @Test
    void cancelsOrderWithoutDeletingHistory() throws Exception {
        OrderTransactionService service = new OrderTransactionService(database);
        long orderId = service.registerOrder(new Order(
                0, OrderPaymentCondition.CREDIT, LocalDateTime.now(),
                List.of(new OrderItem(0, 0, guideId1, 5, new BigDecimal("20.00")))
        ));

        service.cancelOrder(orderId, "Costo equivocado", LocalDateTime.now());

        Order saved = new OrderRepository(database).findById(orderId);
        assertEquals(OrderStatus.CANCELLED, saved.getStatus());
        assertEquals("Costo equivocado", saved.getCancellationReason());
        assertEquals(10, guideRepository.findById(guideId1).getStock());
    }

    @Test
    void correctsOrderUsingOnlyStockDifference() throws Exception {
        OrderTransactionService service = new OrderTransactionService(database);
        long orderId = service.registerOrder(new Order(
                0, OrderPaymentCondition.CREDIT, LocalDateTime.now(),
                List.of(new OrderItem(0, 0, guideId1, 5, new BigDecimal("20.00")))
        ));

        long replacementId = service.correctOrder(
                orderId,
                new Order(0, OrderPaymentCondition.CREDIT, LocalDateTime.now(),
                        List.of(new OrderItem(0, 0, guideId1, 3, new BigDecimal("18.00")))),
                "Cantidad y costo corregidos",
                LocalDateTime.now()
        );

        OrderRepository orders = new OrderRepository(database);
        assertEquals(OrderStatus.CORRECTED, orders.findById(orderId).getStatus());
        assertEquals(OrderStatus.ACTIVE, orders.findById(replacementId).getStatus());
        assertEquals(replacementId, orders.findById(orderId).getReplacementOrderId());
        assertEquals(13, guideRepository.findById(guideId1).getStock());
    }

    @Test
    void completesPendingCostWithoutAddingStockTwice() throws Exception {
        OrderTransactionService service = new OrderTransactionService(database);
        long pendingId = service.registerOrder(new Order(
                0, OrderPaymentCondition.CREDIT, LocalDateTime.now(),
                List.of(new OrderItem(0, 0, guideId1, 5, BigDecimal.ZERO))
        ));

        assertEquals(15, guideRepository.findById(guideId1).getStock());
        assertEquals(0, new SupplierDebtQueryService(database).calculateCurrentDebt()
                .compareTo(BigDecimal.ZERO));

        service.correctOrder(
                pendingId,
                new Order(0, OrderPaymentCondition.CREDIT, LocalDateTime.now(),
                        List.of(new OrderItem(0, 0, guideId1, 5, new BigDecimal("20.00")))),
                "Costo confirmado por proveedor",
                LocalDateTime.now()
        );

        assertEquals(15, guideRepository.findById(guideId1).getStock());
        assertEquals(0, new SupplierDebtQueryService(database).calculateCurrentDebt()
                .compareTo(new BigDecimal("100.00")));
    }

    @Test
    void rejectsCancellationWhenReceivedStockWasAlreadyConsumed() throws Exception {
        OrderTransactionService service = new OrderTransactionService(database);
        long orderId = service.registerOrder(new Order(
                0, OrderPaymentCondition.PAID, LocalDateTime.now(),
                List.of(new OrderItem(0, 0, guideId1, 5, new BigDecimal("20.00")))
        ));
        Guide guide = guideRepository.findById(guideId1);
        guide.removeStock(12);
        guideRepository.update(guide);

        assertThrows(IllegalStateException.class,
                () -> service.cancelOrder(orderId, "Pedido erróneo", LocalDateTime.now()));
        assertEquals(OrderStatus.ACTIVE, new OrderRepository(database).findById(orderId).getStatus());
    }
}
