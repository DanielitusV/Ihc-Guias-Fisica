package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.ExpenseService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExpenseTransactionService {

    private final Database database;
    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final ExpenseService expenseService;

    public ExpenseTransactionService(Database database) {
        this.database = database;
        this.accountRepository = new AccountRepository(database);
        this.movementRepository = new AccountMovementRepository(database);
        this.expenseService = new ExpenseService();
    }

    public long registerExpense(
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
                    expenseService.registerExpense(
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