package com.litus.guias.persistence;

import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.AccountMovementConcept;
import com.litus.guias.account.AccountMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ManualIncomeTransactionService {
    private final Database database;
    private final AccountRepository accounts;
    private final AccountMovementRepository movements;

    public ManualIncomeTransactionService(Database database) {
        this.database = database;
        accounts = new AccountRepository(database);
        movements = new AccountMovementRepository(database);
    }

    public long register(long accountId, BigDecimal amount, String reason, LocalDateTime createdAt)
            throws Exception {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe debe ser mayor a cero");
        }
        return database.inTransaction(connection -> {
            AcademicTermRepository.requireActiveId(connection);
            var account = accounts.findById(connection, accountId);
            if (account == null) throw new IllegalArgumentException("Cuenta no encontrada");
            account.addIncome(amount);
            accounts.update(connection, account);
            return movements.save(connection, new AccountMovement(0, accountId,
                    AccountMovementType.INCOME, AccountMovementConcept.OTHER,
                    amount, normalizedReason, createdAt));
        });
    }
}
