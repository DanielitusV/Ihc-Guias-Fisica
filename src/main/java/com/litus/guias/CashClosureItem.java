package com.litus.guias;

public class CashClosureItem {

    private long id;
    private long cashClosureId;
    private long guideId;
    private int expectedStock;
    private int countedStock;

    public CashClosureItem(
            long id,
            long cashClosureId,
            long guideId,
            int expectedStock,
            int countedStock
    ) {
       this.id = id;
       this.cashClosureId = cashClosureId;
       this.guideId = guideId;
       this.expectedStock = expectedStock;
       this.countedStock = countedStock;
    }

    public int getDifference() {
        return countedStock - expectedStock;
    }
}
