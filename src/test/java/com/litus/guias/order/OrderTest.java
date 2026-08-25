package com.litus.guias.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderTest {

    @Test
    public void orderMustContainAtLeastOneItem() {
        assertThrows(IllegalArgumentException.class,
                () -> new Order(
                        0,
                        OrderPaymentCondition.CREDIT,
                        LocalDateTime.of(2026, 8, 14, 20, 0),
                        List.of()
                )
        );
    }

    @Test
    public void orderCalculatesTotalCost() {
        OrderItem fisicaI = new OrderItem(
                0, 1, 1,
                10,
                new BigDecimal("20.00")
        );

        OrderItem fisicaII = new OrderItem(
                0, 1, 2,
                5,
                new BigDecimal("30.00")
        );

        Order order = new Order(
                1,
                OrderPaymentCondition.CREDIT,
                LocalDateTime.of(2026, 8, 14, 20, 0),
                List.of(fisicaI, fisicaII)
        );

        assertEquals(
                new BigDecimal("350.00"),
                order.getTotalCost()
        );
    }

    @Test
    void detectsPendingUnitCosts() {
        Order pending = new Order(
                1, OrderPaymentCondition.CREDIT, LocalDateTime.now(),
                List.of(new OrderItem(0, 1, 1, 5, BigDecimal.ZERO)));
        Order confirmed = new Order(
                2, OrderPaymentCondition.CREDIT, LocalDateTime.now(),
                List.of(new OrderItem(0, 2, 1, 5, new BigDecimal("20"))));

        assertEquals(true, pending.hasPendingCost());
        assertEquals(false, confirmed.hasPendingCost());
    }
}
