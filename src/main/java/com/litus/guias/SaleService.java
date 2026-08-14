package com.litus.guias;

import java.time.LocalDateTime;

public class SaleService {

    public Sale registerSale(
        Guide guide,
        Account account,
        PaymentMethod paymentMethod,
        LocalDateTime createdAt
    ) {
        guide.sellOne();
        account.addIncome(guide.getCurrentPrice());

        return new Sale(
                0,
                guide.getId(),
                guide.getCurrentPrice(),
                paymentMethod,
                createdAt,
                SaleStatus.ACTIVE
        );
    }
}
