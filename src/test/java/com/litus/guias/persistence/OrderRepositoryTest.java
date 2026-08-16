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

import static org.junit.jupiter.api.Assertions.*;

class OrderRepositoryTest {

    @TempDir
    Path tempDir;

    private Database database;
    private long guideId1;
    private long guideId2;

    @BeforeEach
    void setUp() throws Exception {
        database = new Database(
                "jdbc:sqlite:" + tempDir.resolve("test.db")
        );
        database.initialize();

        GuideRepository guideRepository =
                new GuideRepository(database);

        guideId1 = guideRepository.save(
                new Guide(0, "Física I", new BigDecimal("35.00"), 10)
        );

        guideId2 = guideRepository.save(
                new Guide(0, "Física II", new BigDecimal("40.00"), 20)
        );
    }

    @Test
    void savesAndFindsOrderWithItems() throws Exception {
        OrderRepository repository =
                new OrderRepository(database);

        LocalDateTime date =
                LocalDateTime.of(2026, 8, 15, 21, 15);

        Order order = new Order(
                0,
                OrderPaymentCondition.CREDIT,
                date,
                List.of(
                        new OrderItem(
                                0,
                                0,
                                guideId1,
                                5,
                                new BigDecimal("20.00")
                        ),
                        new OrderItem(
                                0,
                                0,
                                guideId2,
                                3,
                                new BigDecimal("25.00")
                        )
                )
        );

        long id = repository.save(order);

        Order found = repository.findById(id);

        assertNotNull(found);
        assertEquals(OrderPaymentCondition.CREDIT,
                found.getPaymentCondition());
        assertEquals(2, found.getItems().size());
    }
}
