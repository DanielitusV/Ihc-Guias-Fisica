package com.litus.guias.account;

import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderPaymentCondition;
import com.litus.guias.order.OrderStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class SupplierDebtCalculatorTest {

    @Test
    public void creditOrderIncreasesSupplierDebt() {
        OrderItem item = new OrderItem(
                0,
                1,
                1,
                10,
                new BigDecimal("20.00")
        );

        Order order = new Order(
                1,
                OrderPaymentCondition.CREDIT,
                LocalDateTime.of(2026, 8, 14, 20, 0),
                List.of(item)
        );

        SupplierDebtCalculator calculator = new SupplierDebtCalculator();

        BigDecimal debt = calculator.calculate(List.of(order));

        assertEquals(new BigDecimal("200.00"), debt);
    }

    @Test
    public void paidOrderDoesNotIncreaseSupplierDebt() {
        OrderItem item = new OrderItem(
                0,
                1,
                1,
                10,
                new BigDecimal("20.00")
        );

        Order order = new Order(
                1,
                OrderPaymentCondition.PAID,
                LocalDateTime.of(2026, 8, 14, 20, 0),
                List.of(item)
        );

        SupplierDebtCalculator calculator = new SupplierDebtCalculator();
        BigDecimal debt = calculator.calculate(List.of(order));

        assertEquals(BigDecimal.ZERO, debt);
    }

    @Test
    public void supplierPaymentReducesDebt() {
        OrderItem item = new OrderItem(
                0,
                1,
                1,
                10,
                new BigDecimal("20.00")
        );

        Order order = new Order(
                1,
                OrderPaymentCondition.CREDIT,
                LocalDateTime.of(2026, 8, 14, 20, 0),
                List.of(item)
        );

        AccountMovement payment = new AccountMovement(
                1,
                1,
                AccountMovementType.EXPENSE,
                AccountMovementConcept.SUPPLIER_PAYMENT,
                new BigDecimal("50.00"),
                "Pago parcial a fotocopiadora",
                LocalDateTime.of(2026,8,15, 10, 0)
        );

        SupplierDebtCalculator calculator = new SupplierDebtCalculator();
        BigDecimal debt = calculator.calculate(
                List.of(order),
                List.of(payment)
        );

        assertEquals(new BigDecimal("150.00"), debt);
    }

    @Test
    public void supplierDebtCannotBeNegative() {
        OrderItem item = new OrderItem(
                0,
                1,
                1,
                10,
                new BigDecimal("20.00")
        );

        Order order = new Order(
                1,
                OrderPaymentCondition.CREDIT,
                LocalDateTime.of(2026, 8, 14, 20, 0),
                List.of(item)
        );

        AccountMovement payment = new AccountMovement(
                1,
                1,
                AccountMovementType.EXPENSE,
                AccountMovementConcept.SUPPLIER_PAYMENT,
                new BigDecimal("250.00"),
                "Pago a fotocopiadora",
                LocalDateTime.of(2026,8,15, 10, 0)
        );

        SupplierDebtCalculator calculator = new SupplierDebtCalculator();

        assertThrows(
                IllegalStateException.class,
                () -> calculator.calculate(
                        List.of(order),
                        List.of(payment)
                )
        );
    }

    @Test
    void cancelledOrderDoesNotIncreaseDebt() {
        Order order = new Order(
                1,
                OrderPaymentCondition.CREDIT,
                LocalDateTime.of(2026, 8, 14, 20, 0),
                List.of(new OrderItem(0, 1, 1, 10, new BigDecimal("20.00"))),
                OrderStatus.CANCELLED,
                "Registro equivocado",
                LocalDateTime.of(2026, 8, 15, 8, 0),
                null
        );

        assertEquals(BigDecimal.ZERO, new SupplierDebtCalculator().calculate(List.of(order)));
    }
}
