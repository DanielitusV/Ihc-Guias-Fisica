package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.SupplierPaymentService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SupplierPaymentTransactionService {

    private final Database database;
    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final SupplierPaymentService supplierPaymentService;
    private final SupplierDebtQueryService debtQueryService;

    public SupplierPaymentTransactionService(Database database) {
        this.database = database;
        this.accountRepository = new AccountRepository(database);
        this.movementRepository = new AccountMovementRepository(database);
        this.supplierPaymentService = new SupplierPaymentService();
        this.debtQueryService = new SupplierDebtQueryService(database);
    }

    public long registerPayment(
            long accountId,
            BigDecimal amount,
            String reason,
            LocalDateTime createdAt
    ) throws Exception {

        return database.inTransaction(connection -> {

            BigDecimal debt = debtQueryService.calculateCurrentDebt(connection);
            if (amount.compareTo(debt) > 0) {
                throw new IllegalArgumentException(
                        "Supplier payment cannot exceed current debt"
                );
            }

            Account account =
                    accountRepository.findById(connection, accountId);

            if (account == null) {
                throw new IllegalArgumentException("Account not found");
            }

            AccountMovement movement =
                    supplierPaymentService.registerPayment(
                            account,
                            amount,
                            reason,
                            createdAt
                    );

            accountRepository.update(connection, account);

            return movementRepository.save(
                    connection,
                    movement
            );
        });
    }
}
