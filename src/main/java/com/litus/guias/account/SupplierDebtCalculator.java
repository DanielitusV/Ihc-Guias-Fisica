package com.litus.guias.account;

import com.litus.guias.order.Order;
import com.litus.guias.order.OrderPaymentCondition;

import java.math.BigDecimal;
import java.util.List;

public class SupplierDebtCalculator {

    public BigDecimal calculate(List<Order> orders) {
        return calculate(orders, List.of());
    }

    public BigDecimal calculate(List<Order> orders, List<AccountMovement> movements) {
        BigDecimal debt = BigDecimal.ZERO;

        for (Order order : orders) {
            if (order.getPaymentCondition() == OrderPaymentCondition.CREDIT) {
                debt = debt.add(order.getTotalCost());
            }
        }

        for (AccountMovement movement : movements) {
            if (movement.getConcept() == AccountMovementConcept.SUPPLIER_PAYMENT) {
                debt = debt.subtract(movement.getAmount());
            }
        }

        if (debt.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(
                    "Supplier debt cannot be negative"
            );
        }

        return debt;
    }
}
