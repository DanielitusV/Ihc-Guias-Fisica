package com.litus.guias;

public class SaleService {

    public void registerSale(Guide guide, Account account) {
        guide.sellOne();
        account.addIncome(guide.getCurrentPrice());
    }
}
