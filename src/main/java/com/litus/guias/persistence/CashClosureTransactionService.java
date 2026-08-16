package com.litus.guias.persistence;

import com.litus.guias.account.CashClosureItem;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.closure.CashClosureService;

import java.util.List;

public class CashClosureTransactionService {

    private final Database database;
    private final CashClosureRepository repository;
    private final CashClosureService closureService;

    public CashClosureTransactionService(Database database) {
        this.database = database;
        this.repository = new CashClosureRepository(database);
        this.closureService = new CashClosureService();
    }

    public long registerClosure(
            CashClosure closure,
            List<CashClosureItem> items
    ) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cash closure must contain at least one item"
            );
        }

        return database.inTransaction(connection -> {
            List<CashClosure> existing = repository.findAllByDate(
                    connection,
                    closure.getCreatedAt().toLocalDate()
            );
            closureService.registerClosure(closure, existing);
            return repository.save(connection, closure, items);
        });
    }

    public void cancelClosure(long closureId, String reason) throws Exception {
        database.inTransaction(connection -> {
            CashClosure closure = repository.findById(connection, closureId);
            if (closure == null) {
                throw new IllegalArgumentException("Cash closure not found");
            }
            closure.cancel(reason);
            repository.update(connection, closure);
            return null;
        });
    }
}
