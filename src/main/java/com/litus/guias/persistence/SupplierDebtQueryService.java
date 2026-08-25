package com.litus.guias.persistence;

import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.SupplierDebtCalculator;
import com.litus.guias.order.Order;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public class SupplierDebtQueryService {

    private final Database database;
    private final OrderRepository orderRepository;
    private final AccountMovementRepository movementRepository;
    private final SupplierDebtCalculator debtCalculator;

    public SupplierDebtQueryService(
            Database database
    ) {
        this.database = database;
        this.orderRepository =
                new OrderRepository(database);
        this.movementRepository =
                new AccountMovementRepository(database);
        this.debtCalculator =
                new SupplierDebtCalculator();
    }

    public BigDecimal calculateCurrentDebt()
            throws Exception {

        return database.inTransaction(connection -> {
            return calculateCurrentDebt(connection);
        });
    }

    public BigDecimal calculateCurrentDebt(Connection connection)
            throws Exception {
        List<Order> orders = orderRepository.findAll(connection);
        List<AccountMovement> payments =
                movementRepository.findSupplierPayments(connection);
        return debtCalculator.calculate(orders, payments);
    }
}
