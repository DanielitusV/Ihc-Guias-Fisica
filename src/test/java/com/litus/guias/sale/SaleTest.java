package com.litus.guias.sale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SaleTest {

    private LocalDateTime date;
    private Sale sale;

    @BeforeEach
    public void setUp() {
        date = LocalDateTime.of(2026, 8, 14, 19, 0);

        sale = new Sale(
                1,
                10,
                new BigDecimal("25.00"),
                PaymentMethod.QR,
                date,
                SaleStatus.ACTIVE
        );
    }

    @Test
    public void saleKeepsThePriceUsedAtTheMomentOfSale() {

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

        sale.cancel("Venta registrada por error");

        assertEquals(SaleStatus.CANCELLED, sale.getStatus());
        assertEquals(
                "Venta registrada por error",
                sale.getCancellationReason()
        );
    }

    @Test
    public void cancellingSaleWithoutReasonIsNotAllowed() {
        assertThrows(
                IllegalArgumentException.class,
                () -> sale.cancel("")
        );
    }
}
