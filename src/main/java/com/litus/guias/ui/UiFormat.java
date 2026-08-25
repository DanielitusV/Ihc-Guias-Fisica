package com.litus.guias.ui;

import com.litus.guias.account.AccountMovementConcept;
import com.litus.guias.account.AccountMovementType;
import com.litus.guias.closure.CashClosureStatus;
import com.litus.guias.closure.DayStatus;
import com.litus.guias.order.OrderPaymentCondition;
import com.litus.guias.order.OrderStatus;
import com.litus.guias.inventory.InventoryAdjustmentType;
import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.SaleStatus;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class UiFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private UiFormat() {
    }

    public static String money(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.of("es", "BO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        return "Bs " + format.format(value == null ? BigDecimal.ZERO : value);
    }

    public static String date(LocalDate value) {
        return value == null ? "—" : DATE.format(value);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "—" : DATE_TIME.format(value);
    }

    public static String label(Enum<?> value) {
        if (value == null) return "—";
        if (value == PaymentMethod.CASH) return "Efectivo";
        if (value == PaymentMethod.QR) return "QR / Soto";
        if (value == SaleStatus.ACTIVE) return "Activa";
        if (value == SaleStatus.CANCELLED) return "Anulada";
        if (value == OrderPaymentCondition.PAID) return "Pagado";
        if (value == OrderPaymentCondition.CREDIT) return "Crédito";
        if (value == OrderStatus.ACTIVE) return "Activo";
        if (value == OrderStatus.CANCELLED) return "Anulado";
        if (value == OrderStatus.CORRECTED) return "Corregido";
        if (value == InventoryAdjustmentType.CARRYOVER) return "Ingreso de stock";
        if (value == InventoryAdjustmentType.OMITTED_STOCK) return "Stock omitido";
        if (value == InventoryAdjustmentType.COUNT_CORRECTION) return "Corrección de conteo";
        if (value == InventoryAdjustmentType.LOSS_OR_DAMAGE) return "Pérdida o daño";
        if (value == InventoryAdjustmentType.OTHER) return "Otro ajuste";
        if (value == AccountMovementType.INCOME) return "Ingreso";
        if (value == AccountMovementType.EXPENSE) return "Egreso";
        if (value == AccountMovementConcept.SALE) return "Venta";
        if (value == AccountMovementConcept.GENERAL_EXPENSE) return "Gasto general";
        if (value == AccountMovementConcept.SUPPLIER_PAYMENT) return "Pago proveedor";
        if (value == AccountMovementConcept.SALE_CANCELLATION) return "Anulación de venta";
        if (value == AccountMovementConcept.CLOSURE_ADJUSTMENT) return "Ajuste de cierre";
        if (value == AccountMovementConcept.TRANSFER) return "Transferencia";
        if (value == AccountMovementConcept.OTHER) return "Otro";
        if (value == CashClosureStatus.VALID) return "Válido";
        if (value == CashClosureStatus.CANCELLED) return "Anulado";
        if (value == DayStatus.OPEN) return "Abierto";
        if (value == DayStatus.CLOSED) return "Cerrado";
        if (value == DayStatus.MISSED) return "Sin cierre";
        if (value == DayStatus.NO_ACTIVITY) return "Sin actividad";
        return value.name();
    }
}
