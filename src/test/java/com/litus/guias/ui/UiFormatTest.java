package com.litus.guias.ui;

import com.litus.guias.account.AccountMovementConcept;

import com.litus.guias.sale.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiFormatTest {

    @Test
    void labelsTransfersClearly() {
        assertEquals("Transferencia", UiFormat.label(AccountMovementConcept.TRANSFER));
    }

    @Test
    void formatsMoneyAndDomainLabelsInSpanish() {
        assertEquals("Bs 1.234,50", UiFormat.money(new BigDecimal("1234.5")));
        assertEquals("QR / Soto", UiFormat.label(PaymentMethod.QR));
        assertEquals("Efectivo", UiFormat.label(PaymentMethod.CASH));
    }

    @Test
    void formatsDateTimeForOperators() {
        assertEquals(
                "16/08/2026 21:07",
                UiFormat.dateTime(LocalDateTime.of(2026, 8, 16, 21, 7))
        );
    }
}
