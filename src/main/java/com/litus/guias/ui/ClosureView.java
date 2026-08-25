package com.litus.guias.ui;

import com.litus.guias.account.Account;
import com.litus.guias.account.CashClosureItem;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.closure.CashClosureStatus;
import com.litus.guias.inventory.Guide;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClosureView extends VBox implements RefreshableView {
    private final AppContext context;
    private final Label expectedCash = new Label();
    private final Label expectedQr = new Label();
    private final TextField countedCash = new TextField();
    private final TextField reportedQr = new TextField();
    private final GridPane stockGrid = new GridPane();
    private final Map<Long, TextField> countedStock = new LinkedHashMap<>();
    private final TextArea notes = new TextArea();
    private final TableView<CashClosure> table = new TableView<>();
    private final DatePicker closureDate = new DatePicker(LocalDate.now());
    private final Label dateHelp = new Label();
    private com.litus.guias.persistence.ClosureSnapshotQueryService.Snapshot currentSnapshot;

    public ClosureView(AppContext context) {
        this.context = context;
        getStyleClass().addAll("page", "closure-page");
        setPadding(new Insets(22));
        setSpacing(18);
        countedCash.setPromptText("Efectivo contado");
        reportedQr.setPromptText("Saldo QR reportado");
        notes.setPromptText("Observaciones opcionales");
        notes.setPrefRowCount(2);
        closureDate.setEditable(false);
        closureDate.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item.isAfter(LocalDate.now()));
            }
        });
        closureDate.valueProperty().addListener((obs, old, value) -> refreshForSelectedDate());
        dateHelp.getStyleClass().add("closure-date-help");
        dateHelp.setWrapText(true);
        stockGrid.setHgap(12); stockGrid.setVgap(9);
        var register = UiKit.primary("Registrar cierre");
        register.setOnAction(event -> registerClosure());
        HBox money = new HBox(14,
                UiKit.card("Efectivo esperado", expectedCash, countedCash),
                UiKit.card("QR esperado", expectedQr, reportedQr));
        configureTable();
        var cancel = UiKit.danger("Anular cierre seleccionado");
        cancel.setOnAction(event -> cancelClosure());
        getChildren().addAll(
                UiKit.title("Cierre de jornada"),
                UiKit.card("Fecha que estás cerrando", closureDate, dateHelp),
                money,
                UiKit.card("Conteo de inventario", stockGrid),
                UiKit.card("Notas y confirmación", notes, UiKit.actions(register)),
                UiKit.card("Cierres anteriores", table, UiKit.actions(cancel))
        );
        table.setPrefHeight(300);
        context.onRefresh(this::refresh);
        refresh();
    }

    private void registerClosure() {
        try {
            LocalDate selectedDate = validatedDate();
            if (context.dayStatus.getStatus(selectedDate, LocalDate.now()) == com.litus.guias.closure.DayStatus.CLOSED) {
                throw new IllegalStateException("La fecha seleccionada ya tiene un cierre válido");
            }
            currentSnapshot = context.closureSnapshots.atEndOf(selectedDate);
            BigDecimal cashValue = decimalOrZero(countedCash.getText(), "El efectivo contado");
            BigDecimal qrValue = decimalOrZero(reportedQr.getText(), "El QR reportado");
            List<CashClosureItem> items = new ArrayList<>();
            List<Guide> guides = context.guides.findAll();
            for (Guide guide : guides) {
                int counted = UiKit.nonNegativeInt(countedStock.get(guide.getId()).getText(), "El stock contado de " + guide.getName());
                int expected = currentSnapshot.expectedStock().getOrDefault(guide.getId(), 0);
                items.add(new CashClosureItem(0, 0, guide.getId(), expected, counted));
            }
            CashClosure closure = new CashClosure(
                    0, currentSnapshot.expectedCash(), cashValue,
                    currentSnapshot.expectedQr(), qrValue, notes.getText().trim(),
                    selectedDate, LocalDateTime.now()
            );
            String differences = "Diferencia efectivo: " + UiFormat.money(closure.getCashDifference()) + "\nDiferencia QR: " + UiFormat.money(closure.getQrDifference());
            String dateLine = "Fecha operativa: " + UiFormat.date(selectedDate) + "\n";
            if (!UiKit.confirm("Confirmar cierre", dateLine + differences)) return;
            boolean reconcileInventory = false;
            String adjustmentReason = null;
            boolean hasStockDifference = items.stream().anyMatch(item -> item.getDifference() != 0);
            if (hasStockDifference && UiKit.confirm(
                    "Regularizar inventario",
                    "Conteo físico difiere del sistema. ¿Deseas usar cantidades contadas como nuevo stock? Cada cambio quedará en historial.")) {
                var reason = UiKit.reason("Motivo del ajuste", "Explica diferencia de inventario:");
                if (reason.isEmpty()) return;
                reconcileInventory = true;
                adjustmentReason = "Cierre " + UiFormat.date(selectedDate) + ": " + reason.get();
            }
            context.closureTransactions.registerClosure(
                    closure, items, reconcileInventory, adjustmentReason);
            String result = closure.isBalanced(items) ? "La jornada quedó cuadrada"
                    : reconcileInventory ? "Cierre guardado y stock regularizado"
                    : "Cierre guardado con diferencias; stock no fue modificado";
            UiKit.info("Cierre registrado", result);
            context.refreshAll();
        } catch (Exception exception) { UiKit.error("No se pudo registrar el cierre", exception); }
    }

    private BigDecimal decimalOrZero(String text, String field) {
        if (text == null || text.isBlank()) return BigDecimal.ZERO;
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(',', '.'));
            if (value.signum() < 0) throw new NumberFormatException();
            return value;
        } catch (Exception exception) { throw new IllegalArgumentException(field + " debe ser cero o mayor"); }
    }

    private void cancelClosure() {
        CashClosure selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { UiKit.error("Selecciona un cierre", "Debes seleccionar el cierre a anular"); return; }
        if (selected.getStatus() == CashClosureStatus.CANCELLED) { UiKit.error("Cierre ya anulado", "Este cierre ya está anulado"); return; }
        UiKit.reason("Anular cierre", "Motivo obligatorio:").ifPresent(reason -> {
            if (!UiKit.confirm("Confirmar anulación", "La jornada volverá a quedar abierta.")) return;
            try { context.closureTransactions.cancelClosure(selected.getId(), reason); context.refreshAll(); }
            catch (Exception exception) { UiKit.error("No se pudo anular el cierre", exception); }
        });
    }

    private void configureTable() {
        table.getColumns().addAll(
                column("Fecha cerrada", value -> UiFormat.date(value.getBusinessDate())),
                column("Registrado", value -> UiFormat.dateTime(value.getCreatedAt())),
                column("Efectivo esperado", value -> UiFormat.money(value.getExpectedCash())),
                column("Efectivo contado", value -> UiFormat.money(value.getCountedCash())),
                column("QR esperado", value -> UiFormat.money(value.getExpectedQr())),
                column("QR reportado", value -> UiFormat.money(value.getReportedQr())),
                column("Estado", value -> UiFormat.label(value.getStatus()))
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Aún no hay cierres"));
    }

    private TableColumn<CashClosure, String> column(String title, java.util.function.Function<CashClosure, String> getter) {
        TableColumn<CashClosure, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        return column;
    }

    @Override
    public void refresh() {
        refreshForSelectedDate();
        refreshHistory();
    }

    public void selectDate(LocalDate date) {
        closureDate.setValue(date);
        refreshForSelectedDate();
    }

    private LocalDate validatedDate() throws Exception {
        LocalDate date = closureDate.getValue();
        if (date == null) throw new IllegalArgumentException("Selecciona la fecha del cierre");
        if (date.isAfter(LocalDate.now())) throw new IllegalArgumentException("No se puede cerrar una fecha futura");
        var active = context.terms.findActive();
        if (active != null && date.isBefore(active.openedAt().toLocalDate())) {
            throw new IllegalArgumentException("La fecha es anterior al inicio de la gestión activa");
        }
        return date;
    }

    private void refreshForSelectedDate() {
        try {
            LocalDate date = validatedDate();
            currentSnapshot = context.closureSnapshots.atEndOf(date);
            expectedCash.setText(UiFormat.money(currentSnapshot.expectedCash()));
            expectedQr.setText(UiFormat.money(currentSnapshot.expectedQr()));
            countedCash.setText(currentSnapshot.expectedCash().toPlainString());
            reportedQr.setText(currentSnapshot.expectedQr().toPlainString());
            rebuildStock(context.guides.findAll());
            dateHelp.setText(date.equals(LocalDate.now())
                    ? "Cierre de hoy. Verifica dinero y stock contados antes de guardar."
                    : "Cierre pendiente del " + UiFormat.date(date)
                            + ". Los valores esperados fueron reconstruidos hasta el final de ese día; los datos posteriores no cambian.");
            dateHelp.getStyleClass().remove("closure-date-error");
        } catch (Exception exception) {
            dateHelp.setText(exception.getMessage());
            if (!dateHelp.getStyleClass().contains("closure-date-error")) dateHelp.getStyleClass().add("closure-date-error");
        }
    }

    private void rebuildStock(List<Guide> guides) {
        stockGrid.getChildren().clear(); countedStock.clear();
        stockGrid.addRow(0, new Label("Guía"), new Label("Esperado"), new Label("Contado"), new Label("Diferencia"));
        int row = 1;
        for (Guide guide : guides) {
            int expected = currentSnapshot == null ? guide.getStock()
                    : currentSnapshot.expectedStock().getOrDefault(guide.getId(), 0);
            TextField counted = new TextField(String.valueOf(expected));
            Label difference = new Label("0");
            counted.textProperty().addListener((obs, old, value) -> {
                try { difference.setText(String.valueOf(Integer.parseInt(value) - expected)); }
                catch (Exception ignored) { difference.setText("Inválido"); }
            });
            countedStock.put(guide.getId(), counted);
            stockGrid.addRow(row++, new Label(guide.getName()), new Label(String.valueOf(expected)), counted, difference);
        }
    }

    private void refreshHistory() {
        try {
            ArrayList<CashClosure> closures = new ArrayList<>(context.closures.findAll());
            Collections.reverse(closures);
            table.setItems(FXCollections.observableArrayList(closures));
        } catch (Exception exception) { UiKit.error("No se pudo actualizar el cierre", exception); }
    }
}
