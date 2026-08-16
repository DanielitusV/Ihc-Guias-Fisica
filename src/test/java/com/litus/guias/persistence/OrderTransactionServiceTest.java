package com.litus.guias.persistence;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
