package com.litus.guias;

import java.time.LocalDateTime;

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
}
