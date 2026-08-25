package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.AccountMovementConcept;
import com.litus.guias.account.AccountMovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class MoneyTransferTransactionService {
    private final Database database;
    private final AccountRepository accounts;
    private final AccountMovementRepository movements;
    private final CashClosureRepository closures;

    public MoneyTransferTransactionService(Database database) {
        this.database = database;
        accounts = new AccountRepository(database);
        movements = new AccountMovementRepository(database);
        closures = new CashClosureRepository(database);
    }

    public void transfer(long sourceId, long destinationId, BigDecimal amount,
            String reason, LocalDateTime createdAt) throws Exception {
        if (sourceId == destinationId) throw new IllegalArgumentException("Las cuentas deben ser diferentes");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("El importe debe ser mayor a cero");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
        database.inTransaction(connection -> {
            if (closures.findValidByDate(connection, createdAt.toLocalDate()) != null) {
                throw new IllegalStateException("No se puede transferir dinero en una jornada cerrada");
            }
            Account source = accounts.findById(connection, sourceId);
            Account destination = accounts.findById(connection, destinationId);
            if (source == null || destination == null) throw new IllegalArgumentException("Cuenta no encontrada");
            if (source.getBalance().compareTo(amount) < 0) {
                throw new IllegalStateException("Saldo insuficiente en " + source.getName());
            }
            source.addExpense(amount);
            destination.addIncome(amount);
            accounts.update(connection, source);
            accounts.update(connection, destination);
            movements.save(connection, new AccountMovement(0, sourceId, AccountMovementType.EXPENSE,
                    AccountMovementConcept.TRANSFER, amount,
                    "Hacia " + destination.getName() + ": " + reason.trim(), createdAt));
            movements.save(connection, new AccountMovement(0, destinationId, AccountMovementType.INCOME,
                    AccountMovementConcept.TRANSFER, amount,
                    "Desde " + source.getName() + ": " + reason.trim(), createdAt));
            return null;
        });
    }
}
