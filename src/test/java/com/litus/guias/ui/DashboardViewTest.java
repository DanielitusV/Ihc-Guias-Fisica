package com.litus.guias.ui;

import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardViewTest {

    @Test
    void quickSaleNoticeExplainsBlockedButtonsBriefly() {
        assertEquals(
                "Las ventas rápidas sin stock permanecen bloqueadas. Actualice el inventario para habilitarlas.",
                DashboardView.QUICK_SALE_NOTICE
        );
    }

    @Test
    void quickSaleLabelMatchesCompactExcelStyle() {
        Guide guide = new Guide(7, "Física II", new BigDecimal("35"), 12);

        String label = DashboardView.quickSaleLabel(guide, PaymentMethod.QR);

        assertEquals("Física II\nQR · Stock 12", label);
        assertEquals("Física II\nEfectivo · Stock 12", DashboardView.quickSaleLabel(guide, PaymentMethod.CASH));
    }

    @Test
    void quickSaleButtonsUseAppFontWithLightPaletteAndDarkText() throws IOException {
        var resource = DashboardViewTest.class.getResourceAsStream("/com/litus/guias/ui/aero.css");
        assertNotNull(resource);
        String css = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        String quickSaleCss = css.substring(
                css.indexOf(".quick-sale-button {"),
                css.indexOf(".quick-sale-status")
        );

        assertTrue(quickSaleCss.contains("-fx-font-family: \"Segoe UI\", Arial, sans-serif;"));
        assertTrue(quickSaleCss.contains("-fx-font-size: 15px;"));
        assertTrue(quickSaleCss.contains("-fx-font-weight: bold;"));
        assertTrue(quickSaleCss.contains("-fx-min-height: 54;"));
        assertTrue(quickSaleCss.contains("-fx-text-fill: #17324a;"));
        assertFalse(quickSaleCss.contains("Segoe UI Semibold"));
        assertFalse(quickSaleCss.contains("#54277f"));
        assertFalse(quickSaleCss.contains("#1590aa"));
        assertFalse(quickSaleCss.contains("#318f47"));
        assertFalse(quickSaleCss.contains("#c87908"));
    }

    @Test
    void quickSaleSupportingTextLooksLikeNormalText() throws IOException {
        var resource = DashboardViewTest.class.getResourceAsStream("/com/litus/guias/ui/aero.css");
        assertNotNull(resource);
        String css = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(css.contains(".quick-sale-note"));
        assertTrue(css.substring(css.indexOf(".quick-sale-note"), css.indexOf(".quick-sale-button"))
                .contains("-fx-font-weight: normal;"));
        assertTrue(css.contains(".quick-sale-status { -fx-text-fill: #243746; -fx-font-weight: normal;"));
    }

    @Test
    void quickSalesUseFourColumnsAtMediumDashboardWidth() {
        assertEquals(1, DashboardView.quickSaleColumns(699));
        assertEquals(2, DashboardView.quickSaleColumns(700));
        assertEquals(4, DashboardView.quickSaleColumns(900));
    }

    @Test
    void safeModeLocksEveryQuickSaleForOneSecondWithoutConfirmation() {
        Guide available = new Guide(7, "Física II", new BigDecimal("35"), 12);
        Guide empty = new Guide(8, "Física III", new BigDecimal("35"), 0);

        assertEquals(1_000, DashboardView.SAFE_MODE_LOCK_MILLIS);
        assertFalse(DashboardView.quickSaleDisabled(available, false));
        assertTrue(DashboardView.quickSaleDisabled(available, true));
        assertTrue(DashboardView.quickSaleDisabled(empty, false));
    }

    @Test
    void safeModeSwitchShowsItsCurrentState() {
        assertEquals("Activado", DashboardView.safeModeStateLabel(true));
        assertEquals("Desactivado", DashboardView.safeModeStateLabel(false));
    }

    @Test
    void confirmedSafeQuickSaleShowsPositivo08() {
        Guide guide = new Guide(7, "Física II", new BigDecimal("35"), 12);
        LocalDateTime time = LocalDateTime.of(2026, 8, 18, 14, 5, 9);

        assertEquals(
                "✓ Positivo 08 · Física II · QR / Soto · Bs 35,00 · 14:05:09",
                DashboardView.quickSaleSuccessStatus(guide, PaymentMethod.QR, time, true));
        assertFalse(DashboardView.quickSaleSuccessStatus(guide, PaymentMethod.QR, time, false)
                .contains("Positivo 08"));
    }

    @Test
    void quickSaleTitleIsHighlightedButSupportingTextIsNotBold() throws IOException {
        var resource = DashboardViewTest.class.getResourceAsStream("/com/litus/guias/ui/aero.css");
        assertNotNull(resource);
        String css = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

        assertTrue(css.contains(".quick-sale-card .card-title { -fx-font-size: 16px; -fx-font-weight: bold;"));
        assertTrue(css.contains(".quick-sale-note"));
        assertTrue(css.substring(css.indexOf(".quick-sale-note"), css.indexOf(".quick-sale-button"))
                .contains("-fx-font-weight: normal;"));
    }

    @Test
    void standardButtonsShareOneHeightWhileSpecialControlsKeepTheirOwnSize() throws IOException {
        var resource = DashboardViewTest.class.getResourceAsStream("/com/litus/guias/ui/aero.css");
        assertNotNull(resource);
        String css = new String(resource.readAllBytes(), StandardCharsets.UTF_8);

        String standardButtonCss = css.substring(css.indexOf(".button {"), css.indexOf(".button.quick-sale-button"));
        assertTrue(standardButtonCss.contains("-fx-min-height: 36;"));
        assertTrue(standardButtonCss.contains("-fx-pref-height: 36;"));
        assertTrue(standardButtonCss.contains("-fx-max-height: 36;"));
        assertTrue(css.contains(".button.quick-sale-button"));
        assertTrue(css.contains("-fx-min-height: 64;"));
        assertTrue(css.contains("-fx-pref-height: 64;"));
        assertTrue(css.contains("-fx-max-height: 64;"));
        assertTrue(css.contains(".button.safe-mode-toggle"));
        assertTrue(css.contains("-fx-pref-height: 26;"));
    }
}
