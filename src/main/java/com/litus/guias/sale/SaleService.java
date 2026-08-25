package com.litus.guias.sale;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.AccountMovementConcept;
import com.litus.guias.account.AccountMovementType;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.closure.CashClosureStatus;
import com.litus.guias.inventory.Guide;

import java.time.LocalDateTime;
import java.util.List;

public class SaleService {

    public SaleResult registerSale(
        Guide guide,
        Account account,
        PaymentMethod paymentMethod,
        LocalDateTime createdAt
    ) {
        guide.sellOne();
        account.addIncome(guide.getCurrentPrice());

        Sale sale = new Sale(
                0,
                guide.getId(),
                account.getId(),
                guide.getCurrentPrice(),
                paymentMethod,
                createdAt,
                SaleStatus.ACTIVE
        );

        AccountMovement movement = new AccountMovement(
                0,
                account.getId(),
                AccountMovementType.INCOME,
                AccountMovementConcept.SALE,
                guide.getCurrentPrice(),
                null,
                createdAt
        );

        return new SaleResult(sale, movement);
    }

    public AccountMovement cancelSale(
            Sale sale,
            Guide guide,
            Account account,
            String reason,
            LocalDateTime createdAt
    ) {
        sale.cancel(reason, createdAt);

        guide.addOne();
        account.addExpense(sale.getPrice());

        return new AccountMovement(
                0,
                account.getId(),
                AccountMovementType.EXPENSE,
                AccountMovementConcept.SALE_CANCELLATION,
                sale.getPrice(),
                reason,
                createdAt
        );
    }

    public SaleResult registerSale(
            Guide guide,
            Account account,
            PaymentMethod paymentMethod,
            LocalDateTime createdAt,
            List<CashClosure> closures
    ) {
        for (CashClosure closure : closures) {
            boolean sameDay =
                    closure.getBusinessDate()
                            .equals(createdAt.toLocalDate());

            boolean valid =
                    closure.getStatus() == CashClosureStatus.VALID;

            boolean afterOrAtClosure =
                    !createdAt.isBefore(closure.getCreatedAt());

            if (sameDay && valid && afterOrAtClosure) {
                throw new IllegalStateException(
                        "Cannot register a sale after a valid cash closure"
                );
            }
        }

        return registerSale(
                guide,
                account,
                paymentMethod,
                createdAt
        );
    }
}
