
package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.SaleResult;
import com.litus.guias.sale.SaleService;

import java.time.LocalDateTime;

public class SaleTransactionService {

    private final Database database;
    private final GuideRepository guideRepository;
    private final AccountRepository accountRepository;
    private final SaleRepository saleRepository;
    private final AccountMovementRepository movementRepository;
    private final SaleService saleService;

    public SaleTransactionService(Database database) {
        this.database = database;
        this.guideRepository = new GuideRepository(database);
        this.accountRepository = new AccountRepository(database);
        this.saleRepository = new SaleRepository(database);
        this.movementRepository = new AccountMovementRepository(database);
        this.saleService = new SaleService();
    }

    public long registerSale(
            long guideId,
            long accountId,
            PaymentMethod paymentMethod,
            LocalDateTime createdAt
    ) throws Exception {

        return database.inTransaction(connection -> {

            Guide guide =
                    guideRepository.findById(connection, guideId);

            Account account =
                    accountRepository.findById(connection, accountId);

            if (guide == null) {
                throw new IllegalArgumentException("Guide not found");
            }

            if (account == null) {
                throw new IllegalArgumentException("Account not found");
            }

            SaleResult result = saleService.registerSale(
                    guide,
                    account,
                    paymentMethod,
                    createdAt
            );

            guideRepository.update(connection, guide);
            accountRepository.update(connection, account);

            long saleId =
                    saleRepository.save(connection, result.getSale());

            movementRepository.save(
                    connection,
                    result.getMovement()
            );

            return saleId;
        });
    }

    public void cancelSale(
            long saleId,
            long accountId,
            String reason,
            LocalDateTime createdAt
    ) throws Exception {

        database.inTransaction(connection -> {

            Sale sale =
                    saleRepository.findById(connection, saleId);

            if (sale == null) {
                throw new IllegalArgumentException("Sale not found");
            }

            Guide guide =
                    guideRepository.findById(connection, sale.getGuideId());

            Account account =
                    accountRepository.findById(connection, accountId);

            if (guide == null) {
                throw new IllegalArgumentException("Guide not found");
            }

            if (account == null) {
                throw new IllegalArgumentException("Account not found");
            }

            AccountMovement movement = saleService.cancelSale(
                    sale,
                    guide,
                    account,
                    reason,
                    createdAt
            );

            saleRepository.update(connection, sale);
            guideRepository.update(connection, guide);
            accountRepository.update(connection, account);
            movementRepository.save(connection, movement);

            return null;
        });
    }
}