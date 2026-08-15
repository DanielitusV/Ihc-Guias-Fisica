package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderServiceTest {

    @Test
    public void registeringOrderIncreasesGuideStock() {
        Guide fisicaI = new Guide(
                1,
                "Física I",
                new BigDecimal("25.00"),
                10
        );

        OrderItem item = new OrderItem(
                0,
                1,
                fisicaI.getId(),
                20,
                new BigDecimal("23.20")
        );

        Order order = new Order(
                1,
                OrderPaymentCondition.CREDIT,
                LocalDateTime.of(2026, 8, 14, 20, 0),
                List.of(item)
        );

        OrderService service = new OrderService();
        service.registerOrder(order, List.of(fisicaI));
        assertEquals(30, fisicaI.getStock());
    }

    @Test
    public void invalidOrderDoesNotPartiallyChangeStock() {
        Guide fisicaI = new Guide(
                1,
                "Física I",
                new BigDecimal("25.00"),
                10
        );

        OrderItem validItem = new OrderItem(
                0,
                1,
                1,
                20,
                new BigDecimal("23.00")
        );

        OrderItem unknownGuideItem = new OrderItem(
                0,
                1,
                999,
                10,
                new BigDecimal("23.00")
        );

        Order order = new Order(
                1,
                OrderPaymentCondition.CREDIT,
                LocalDateTime.of(2026, 8, 14, 21, 0),
                List.of(validItem, unknownGuideItem)
        );

        OrderService service = new OrderService();
        assertThrows(
                IllegalArgumentException.class,
                () -> service.registerOrder(
                        order,
                        List.of(fisicaI)
                )
        );
    }
}
