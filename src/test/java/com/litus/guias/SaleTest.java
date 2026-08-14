package com.litus.guias;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleTest {

    LocalDateTime date = LocalDateTime.of(
            2026, 8, 14,
            19, 0
    );

    @Test
    public void saleKeepsThePriceUsedAtTheMomentOfSale() {
        Sale sale = new Sale(
                1,
                10,
                new BigDecimal("25.00"),
                PaymentMethod.QR,
                date,
                SaleStatus.ACTIVE
        );

        assertEquals(
                new BigDecimal("25.00"),
                sale.getPrice()
        );

        assertEquals(
                PaymentMethod.QR,
                sale.getPaymentMethod()
        );

        assertEquals(date, sale.getCreatedAt());

        assertEquals(SaleStatus.ACTIVE, sale.getStatus());
    }

    @Test
    public void cancellingSaleChangesStatusAndStoresReason() {
        Sale sale = new Sale(
                1,
                10,
                new BigDecimal("25.00"),
                PaymentMethod.QR,
                date,
                SaleStatus.ACTIVE
        );

        sale.cancel("Venta registrada por error");

        assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        assertEquals(
                "Venta registrada por error",
                sale.getCancellationReason()
        );
    }
}
