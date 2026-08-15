package com.litus.guias.closure;

import com.litus.guias.account.CashClosureItem;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CashClosureItemTest {

    @Test
    public void calculatesStockDifference() {
        CashClosureItem item = new CashClosureItem(
                1,
                1,
                2,
                50,
                47
        );

        assertEquals(-3, item.getDifference());
    }
}
