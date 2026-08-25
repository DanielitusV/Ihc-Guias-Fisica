package com.litus.guias.ui;

import com.litus.guias.inventory.Guide;
import com.litus.guias.inventory.InventoryAdjustment;
import com.litus.guias.inventory.InventoryAdjustmentType;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

public final class InventoryView extends VBox implements RefreshableView {
    private final AppContext context;
    private final TableView<Guide> table = new TableView<>();
    private final TextField price = new TextField();
    private final ComboBox<Guide> adjustmentGuide = new ComboBox<>();
    private final ComboBox<InventoryAdjustmentType> adjustmentType = new ComboBox<>();
    private final ComboBox<String> adjustmentDirection = new ComboBox<>();
    private final TextField adjustmentQuantity = new TextField();
    private final TextField adjustmentReason = new TextField();
    private final TableView<InventoryAdjustment> adjustmentHistory = new TableView<>();

    public InventoryView(AppContext context) {
        this.context = context;
        getStyleClass().addAll("page", "inventory-page");
        setPadding(new Insets(22));
        setSpacing(18);
        configureGuideTable();
        configureAdjustmentForm();
        configureAdjustmentHistory();

        price.setPromptText("Nuevo precio en Bs");
        Button update = UiKit.primary("Actualizar precio");
        update.setOnAction(event -> updatePrice());
        VBox priceEditor = UiKit.card("Precio de venta", price, UiKit.actions(update));
        priceEditor.setPrefWidth(360);
        FlowPane editor = new FlowPane(14, 14, priceEditor);

        getChildren().addAll(
                UiKit.title("Inventario"),
                UiKit.card("Stock y precios",
                        new Label("El stock cambia mediante pedidos, ventas y ajustes autorizados."), table),
                UiKit.card("Editar precio de la guía seleccionada", editor),
                adjustmentCard(),
                UiKit.card("Historial de ajustes", adjustmentHistory)
        );
        adjustmentHistory.setPrefHeight(250);
        context.onRefresh(this::refresh);
        refresh();
    }

    private void configureGuideTable() {
        table.getColumns().addAll(
                guideColumn("Guía", Guide::getName),
                guideColumn("Precio actual", guide -> UiFormat.money(guide.getCurrentPrice())),
                guideColumn("Costo unitario", guide -> UiFormat.money(guide.getDefaultUnitCost())),
                guideColumn("Stock", guide -> guide.getStock() + " unidades"),
                guideColumn("Estado", guide -> guide.getStock() <= 5 ? "Stock bajo" : "Disponible")
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(420);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) ->
                price.setText(value == null ? "" : value.getCurrentPrice().toPlainString()));
    }

    private void configureAdjustmentForm() {
        adjustmentGuide.setConverter(new StringConverter<>() {
            public String toString(Guide value) { return value == null ? "" : value.getName(); }
            public Guide fromString(String value) { return null; }
        });
        adjustmentType.setItems(FXCollections.observableArrayList(InventoryAdjustmentType.values()));
        adjustmentType.setConverter(new StringConverter<>() {
            public String toString(InventoryAdjustmentType value) { return UiFormat.label(value); }
            public InventoryAdjustmentType fromString(String value) { return null; }
        });
        adjustmentType.getSelectionModel().select(InventoryAdjustmentType.CARRYOVER);
        adjustmentDirection.setItems(FXCollections.observableArrayList("Entrada", "Salida"));
        adjustmentDirection.getSelectionModel().selectFirst();
        adjustmentQuantity.setPromptText("Cantidad");
        adjustmentReason.setPromptText("Motivo obligatorio");
    }

    private VBox adjustmentCard() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(9);
        form.addRow(0, new Label("Guía"), adjustmentGuide, new Label("Tipo"), adjustmentType);
        form.addRow(1, new Label("Movimiento"), adjustmentDirection,
                new Label("Cantidad"), adjustmentQuantity);
        adjustmentReason.setMaxWidth(Double.MAX_VALUE);
        form.add(new Label("Motivo"), 0, 2);
        form.add(adjustmentReason, 1, 2, 3, 1);
        Button register = UiKit.primary("Registrar ajuste");
        register.setOnAction(event -> registerAdjustment());
        Label help = new Label(
                "Usa Ingreso de stock para guías gratuitas. Aumenta inventario sin crear compra, deuda ni costo pendiente.");
        help.setWrapText(true);
        help.setMaxWidth(Double.MAX_VALUE);
        return UiKit.card("Ajuste autorizado de inventario", help, form, UiKit.actions(register));
    }

    private void configureAdjustmentHistory() {
        adjustmentHistory.getColumns().addAll(
                adjustmentColumn("Fecha", value -> UiFormat.dateTime(value.createdAt())),
                adjustmentColumn("Guía", value -> guideName(value.guideId())),
                adjustmentColumn("Tipo", value -> UiFormat.label(value.type())),
                adjustmentColumn("Cambio", value -> (value.quantityDelta() > 0 ? "+" : "") + value.quantityDelta()),
                adjustmentColumn("Motivo", InventoryAdjustment::reason)
        );
        adjustmentHistory.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        adjustmentHistory.setPlaceholder(new Label("Aún no hay ajustes manuales"));
    }

    private TableColumn<Guide, String> guideColumn(
            String title, java.util.function.Function<Guide, String> getter) {
        TableColumn<Guide, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        return column;
    }

    private TableColumn<InventoryAdjustment, String> adjustmentColumn(
            String title, java.util.function.Function<InventoryAdjustment, String> getter) {
        TableColumn<InventoryAdjustment, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        return column;
    }

    private String guideName(long id) {
        try { return context.queries.guideNames().getOrDefault(id, "—"); }
        catch (Exception exception) { return "—"; }
    }

    private void updatePrice() {
        Guide selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiKit.error("Selecciona una guía", "Debes seleccionar la guía que quieres modificar");
            return;
        }
        try {
            var value = UiKit.decimal(price.getText(), "El precio");
            if (!UiKit.confirm("Actualizar precio",
                    selected.getName() + " pasará a costar " + UiFormat.money(value))) return;
            context.guides.update(new Guide(selected.getId(), selected.getName(), value,
                    selected.getDefaultUnitCost(), selected.getStock()));
            context.refreshAll();
        } catch (Exception exception) {
            UiKit.error("No se pudo actualizar el precio", exception);
        }
    }

    private void registerAdjustment() {
        Guide guide = adjustmentGuide.getValue();
        if (guide == null) {
            UiKit.error("Selecciona una guía", "Debes elegir qué inventario quieres ajustar");
            return;
        }
        try {
            int quantity = UiKit.nonNegativeInt(adjustmentQuantity.getText(), "La cantidad");
            if (quantity == 0) throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
            String reason = adjustmentReason.getText() == null ? "" : adjustmentReason.getText().trim();
            if (reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
            int delta = "Salida".equals(adjustmentDirection.getValue()) ? -quantity : quantity;
            int resultingStock = Math.addExact(guide.getStock(), delta);
            if (resultingStock < 0) throw new IllegalStateException("El ajuste dejaría stock negativo");
            String summary = guide.getName() + ": " + guide.getStock() + " a " + resultingStock
                    + " unidades\nMotivo: " + reason;
            if (!UiKit.confirm("Confirmar ajuste de inventario", summary)) return;
            context.inventoryAdjustmentTransactions.register(
                    guide.getId(), delta, adjustmentType.getValue(), reason,
                    LocalDateTime.now());
            adjustmentQuantity.clear();
            adjustmentReason.clear();
            UiKit.info("Inventario actualizado", "Ajuste guardado en historial");
            context.refreshAll();
        } catch (Exception exception) {
            UiKit.error("No se pudo ajustar el inventario", exception);
        }
    }

    @Override
    public void refresh() {
        try {
            var guideValues = context.guides.findAll();
            table.setItems(FXCollections.observableArrayList(guideValues));
            Guide previous = adjustmentGuide.getValue();
            adjustmentGuide.setItems(FXCollections.observableArrayList(guideValues));
            if (previous != null) adjustmentGuide.getSelectionModel().select(
                    guideValues.stream().filter(value -> value.getId() == previous.getId()).findFirst().orElse(null));
            if (adjustmentGuide.getValue() == null && !guideValues.isEmpty()) {
                adjustmentGuide.getSelectionModel().selectFirst();
            }
            ArrayList<InventoryAdjustment> adjustments =
                    new ArrayList<>(context.inventoryAdjustments.findAll());
            Collections.reverse(adjustments);
            adjustmentHistory.setItems(FXCollections.observableArrayList(adjustments));
        } catch (Exception exception) {
            UiKit.error("No se pudo cargar el inventario", exception);
        }
    }
}
