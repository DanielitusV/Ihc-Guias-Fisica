package com.litus.guias.account;

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

    public long getId() {
        return id;
    }

    public long getCashClosureId() {
        return cashClosureId;
    }

    public long getGuideId() {
        return guideId;
    }

    public int getExpectedStock() {
        return expectedStock;
    }

    public int getCountedStock() {
        return countedStock;
    }
}
