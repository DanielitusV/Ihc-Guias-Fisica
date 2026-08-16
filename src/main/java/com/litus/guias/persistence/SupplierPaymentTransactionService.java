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

    public SupplierPaymentTransactionService(Database database) {
        this.database = database;
        this.accountRepository = new AccountRepository(database);
        this.movementRepository = new AccountMovementRepository(database);
        this.supplierPaymentService = new SupplierPaymentService();
    }

    public long registerPayment(
            long accountId,
            BigDecimal amount,
            String reason,
            LocalDateTime createdAt
    ) throws Exception {

        return database.inTransaction(connection -> {

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
