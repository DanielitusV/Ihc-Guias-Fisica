package com.litus.guias.ui;

import com.litus.guias.account.Account;
import com.litus.guias.persistence.AccountRepository;
import com.litus.guias.persistence.Database;
import com.litus.guias.persistence.SaleTransactionService;
import com.litus.guias.sale.PaymentMethod;

import java.time.LocalDateTime;

final class QuickSaleService {
    private static final String CASH_ACCOUNT = "Efectivo";
    private static final String QR_ACCOUNT = "QR / Soto";

    private final AccountRepository accounts;
    private final SaleTransactionService sales;

    QuickSaleService(Database database) {
        accounts = new AccountRepository(database);
        sales = new SaleTransactionService(database);
    }

    long register(long guideId, PaymentMethod method, LocalDateTime createdAt)
            throws Exception {
        Account account = accounts.findByName(
                method == PaymentMethod.CASH ? CASH_ACCOUNT : QR_ACCOUNT
        );
        if (account == null) {
            throw new IllegalStateException(
                    "No existe la cuenta requerida para " + UiFormat.label(method)
            );
        }
        return sales.registerSale(guideId, account.getId(), method, createdAt);
    }

    void undo(long saleId, LocalDateTime createdAt) throws Exception {
        sales.cancelSale(
                saleId,
                "Venta rápida deshecha desde el dashboard",
                createdAt
        );
    }
}
