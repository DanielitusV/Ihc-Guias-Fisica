package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.SaleResult;
import com.litus.guias.sale.SaleService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SaleTransactionService {

    private final Database database;
    private final GuideRepository guideRepository;
    private final AccountRepository accountRepository;
    private final SaleRepository saleRepository;
    private final AccountMovementRepository movementRepository;
    private final CashClosureRepository closureRepository;
    private final SaleService saleService;

    public SaleTransactionService(Database database) {
        this.database = database;
        this.guideRepository = new GuideRepository(database);
        this.accountRepository = new AccountRepository(database);
        this.saleRepository = new SaleRepository(database);
        this.movementRepository =
                new AccountMovementRepository(database);
        this.closureRepository =
                new CashClosureRepository(database);
        this.saleService = new SaleService();
    }

    public long registerSale(
            long guideId,
            long accountId,
            PaymentMethod paymentMethod,
            LocalDateTime createdAt
    ) throws Exception {
        return registerSales(Map.of(guideId, 1), accountId, paymentMethod, createdAt).getFirst();
    }

    public List<Long> registerSales(Map<Long, Integer> quantities, long accountId,
            PaymentMethod paymentMethod, LocalDateTime createdAt) throws Exception {
        if (quantities == null || quantities.isEmpty()) {
            throw new IllegalArgumentException("Añade al menos una guía");
        }
        if (quantities.values().stream().anyMatch(value -> value == null || value <= 0)) {
            throw new IllegalArgumentException("Las cantidades deben ser mayores a cero");
        }
        return database.inTransaction(connection -> {
            Account account = accountRepository.findById(connection, accountId);
            if (account == null) throw new IllegalArgumentException("Account not found");
            CashClosure closure = closureRepository.findValidByDate(connection, createdAt.toLocalDate());
            List<CashClosure> closures = closure == null ? List.of() : List.of(closure);
            Map<Long, Guide> guides = new LinkedHashMap<>();
            for (var entry : quantities.entrySet()) {
                Guide guide = guideRepository.findById(connection, entry.getKey());
                if (guide == null) throw new IllegalArgumentException("Guide not found");
                if (guide.getStock() < entry.getValue()) {
                    throw new IllegalStateException("No hay stock suficiente de " + guide.getName());
                }
                guides.put(entry.getKey(), guide);
            }
            List<Long> ids = new ArrayList<>();
            for (var entry : quantities.entrySet()) {
                Guide guide = guides.get(entry.getKey());
                for (int unit = 0; unit < entry.getValue(); unit++) {
                    SaleResult result = saleService.registerSale(
                            guide, account, paymentMethod, createdAt, closures);
                    ids.add(saleRepository.save(connection, result.getSale()));
                    movementRepository.save(connection, result.getMovement());
                }
            }
            for (Guide guide : guides.values()) guideRepository.update(connection, guide);
            accountRepository.update(connection, account);
            return List.copyOf(ids);
        });
    }

    public void cancelSale(
            long saleId,
            long accountId,
            String reason,
            LocalDateTime createdAt
    ) throws Exception {

        Sale sale = saleRepository.findById(saleId);
        if (sale == null) {
            throw new IllegalArgumentException("Sale not found");
        }
        if (sale.getAccountId() != accountId) {
            throw new IllegalArgumentException(
                    "Sale does not belong to supplied account"
            );
        }
        cancelSale(saleId, reason, createdAt);
    }

    public void cancelSale(
            long saleId,
            String reason,
            LocalDateTime createdAt
    ) throws Exception {

        database.inTransaction(connection -> {

            Sale sale =
                    saleRepository.findById(
                            connection,
                            saleId
                    );

            if (sale == null) {
                throw new IllegalArgumentException(
                        "Sale not found"
                );
            }

            Guide guide =
                    guideRepository.findById(
                            connection,
                            sale.getGuideId()
                    );

            Account account =
                    accountRepository.findById(
                            connection,
                            sale.getAccountId()
                    );

            if (guide == null) {
                throw new IllegalArgumentException(
                        "Guide not found"
                );
            }

            if (account == null) {
                throw new IllegalArgumentException(
                        "Account not found"
                );
            }

            AccountMovement movement =
                    saleService.cancelSale(
                            sale,
                            guide,
                            account,
                            reason,
                            createdAt
                    );

            saleRepository.update(
                    connection,
                    sale
            );

            guideRepository.update(
                    connection,
                    guide
            );

            accountRepository.update(
                    connection,
                    account
            );

            movementRepository.save(
                    connection,
                    movement
            );

            return null;
        });
    }
}
