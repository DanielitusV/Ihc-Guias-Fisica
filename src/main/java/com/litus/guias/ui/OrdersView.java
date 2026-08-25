package com.litus.guias.ui;

import com.litus.guias.inventory.Guide;
import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderPaymentCondition;
import com.litus.guias.order.OrderStatus;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OrdersView extends VBox implements RefreshableView {
    private final AppContext context;
    private final GridPane itemsGrid = new GridPane();
    private final Map<Long, TextField> quantities = new LinkedHashMap<>();
    private final Map<Long, TextField> costs = new LinkedHashMap<>();
    private final ComboBox<OrderPaymentCondition> condition = new ComboBox<>();
    private final Label total = new Label(UiFormat.money(BigDecimal.ZERO));
    private final TableView<Order> table = new TableView<>();
    private final Button submit = UiKit.primary("Registrar pedido");
    private final Button cancelCorrection = new Button("Cancelar corrección");
    private final Label correctionMode = new Label();
    private Long correctedOrderId;

    public OrdersView(AppContext context) {
        this.context = context;
        getStyleClass().addAll("page", "orders-page");
        setPadding(new Insets(22));
        setSpacing(18);
        itemsGrid.setHgap(12);
        itemsGrid.setVgap(10);
        condition.setItems(FXCollections.observableArrayList(OrderPaymentCondition.PAID, OrderPaymentCondition.CREDIT));
        condition.setConverter(new StringConverter<>() {
            public String toString(OrderPaymentCondition value) { return UiFormat.label(value); }
            public OrderPaymentCondition fromString(String value) { return null; }
        });
        condition.getSelectionModel().selectFirst();
        submit.setOnAction(event -> registerOrder());
        cancelCorrection.setOnAction(event -> finishCorrection());
        cancelCorrection.setVisible(false);
        cancelCorrection.setManaged(false);
        correctionMode.getStyleClass().add("orders-correction-mode");
        correctionMode.setWrapText(true);
        correctionMode.setMaxWidth(Double.MAX_VALUE);
        correctionMode.setVisible(false);
        correctionMode.setManaged(false);
        Button saveCosts = new Button("Guardar costos unitarios");
        saveCosts.getStyleClass().add("save-unit-costs-button");
        saveCosts.setOnAction(event -> saveDefaultCosts());
        HBox conditionBox = new HBox(8, new Label("Condición:"), condition);
        conditionBox.setAlignment(Pos.CENTER_LEFT);
        HBox totalBox = new HBox(8, new Label("Total:"), total, submit, cancelCorrection);
        totalBox.setAlignment(Pos.CENTER_LEFT);
        FlowPane footer = new FlowPane(12, 8, conditionBox, saveCosts, totalBox);
        footer.getStyleClass().add("orders-footer");
        Label costsHelp = new Label(
                "Costo 0 significa pendiente: el stock ingresará ahora y podrás completar el costo después sin duplicar unidades.");
        costsHelp.setWrapText(true);
        costsHelp.setMaxWidth(Double.MAX_VALUE);
        costsHelp.getStyleClass().add("orders-help");
        configureTable();
        getChildren().addAll(
                UiKit.title("Pedidos"),
                UiKit.card("Nuevo pedido", costsHelp, correctionMode, itemsGrid, footer),
                orderHistoryCard()
        );
        table.setPrefHeight(360);
        context.onRefresh(this::refresh);
        refresh();
    }

    private VBox orderHistoryCard() {
        Button correct = new Button("Completar o corregir pedido");
        correct.setOnAction(event -> beginCorrection());
        Button cancel = UiKit.danger("Anular pedido seleccionado");
        cancel.setOnAction(event -> cancelSelectedOrder());
        return UiKit.card("Historial de pedidos", table, UiKit.actions(correct, cancel));
    }

    private void rebuildItems(List<Guide> guides) {
        itemsGrid.getChildren().clear();
        quantities.clear();
        costs.clear();
        itemsGrid.addRow(0, header("Guía"), header("Cantidad"), header("Costo unitario"), header("Subtotal"));
        int row = 1;
        for (Guide guide : guides) {
            TextField quantity = new TextField("0");
            TextField cost = new TextField(guide.getDefaultUnitCost().toPlainString());
            quantity.setPrefColumnCount(7);
            cost.setPrefColumnCount(9);
            cost.setPromptText("0,00");
            Label subtotal = new Label(UiFormat.money(BigDecimal.ZERO));
            Runnable calculate = () -> {
                try {
                    int q = Integer.parseInt(quantity.getText().trim());
                    BigDecimal c = cost.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(cost.getText().trim().replace(',', '.'));
                    subtotal.setText(UiFormat.money(c.multiply(BigDecimal.valueOf(Math.max(0, q)))));
                } catch (Exception ignored) {
                    subtotal.setText("Valor inválido");
                }
                calculateTotal();
            };
            quantity.textProperty().addListener((obs, old, value) -> calculate.run());
            cost.textProperty().addListener((obs, old, value) -> calculate.run());
            quantities.put(guide.getId(), quantity);
            costs.put(guide.getId(), cost);
            itemsGrid.addRow(row++, new Label(guide.getName()), quantity, cost, subtotal);
        }
    }

    private Label header(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private void calculateTotal() {
        BigDecimal value = BigDecimal.ZERO;
        try {
            for (Long guideId : quantities.keySet()) {
                int quantity = Integer.parseInt(quantities.get(guideId).getText().trim());
                if (quantity <= 0) continue;
                BigDecimal cost = new BigDecimal(costs.get(guideId).getText().trim().replace(',', '.'));
                value = value.add(cost.multiply(BigDecimal.valueOf(quantity)));
            }
            total.setText(UiFormat.money(value));
        } catch (Exception exception) {
            total.setText("Revisa los valores");
        }
    }

    private void saveDefaultCosts() {
        try {
            Map<Long, BigDecimal> values = new LinkedHashMap<>();
            Map<Long, String> names = context.queries.guideNames();
            for (var entry : costs.entrySet()) {
                values.put(entry.getKey(), UiKit.nonNegativeDecimal(
                        entry.getValue().getText(), "El costo de " + names.get(entry.getKey())));
            }
            if (!UiKit.confirm("Guardar costos unitarios",
                    "Estos valores se usarán automáticamente en los próximos pedidos. ¿Deseas guardarlos?")) return;
            context.guides.updateDefaultUnitCosts(values);
            UiKit.info("Costos guardados", "Los costos unitarios quedaron guardados correctamente");
        } catch (Exception exception) {
            UiKit.error("No se pudieron guardar los costos unitarios", exception);
        }
    }

    private void registerOrder() {
        try {
            List<OrderItem> items = new ArrayList<>();
            Map<Long, String> names = context.queries.guideNames();
            for (Long guideId : quantities.keySet()) {
                int quantity = UiKit.nonNegativeInt(quantities.get(guideId).getText(), "La cantidad de " + names.get(guideId));
                if (quantity == 0) continue;
                BigDecimal cost = UiKit.nonNegativeDecimal(
                        costs.get(guideId).getText(), "El costo de " + names.get(guideId));
                items.add(new OrderItem(0, 0, guideId, quantity, cost));
            }
            if (items.isEmpty()) throw new IllegalArgumentException("Añade al menos una guía con cantidad mayor a cero");
            Order order = new Order(0, condition.getValue(), LocalDateTime.now(), items);
            String pending = order.hasPendingCost()
                    ? "\nContiene costos pendientes. El stock ingresará ahora; la deuda se completará al registrar costos."
                    : "";
            if (!UiKit.confirm("Confirmar pedido",
                    UiFormat.label(order.getPaymentCondition()) + " por "
                            + UiFormat.money(order.getTotalCost()) + pending)) return;
            if (correctedOrderId == null) {
                context.orderTransactions.registerOrder(order);
                UiKit.info("Pedido registrado", order.hasPendingCost()
                        ? "Stock actualizado. Pedido marcado con costo pendiente"
                        : "El stock fue actualizado");
            } else {
                var reason = UiKit.reason("Motivo de corrección", "Explica qué dato estaba equivocado:");
                if (reason.isEmpty()) return;
                context.orderTransactions.correctOrder(
                        correctedOrderId, order, reason.get(), LocalDateTime.now());
                UiKit.info("Pedido corregido", "Stock y deuda fueron recalculados; registro anterior quedó en historial");
                finishCorrection();
            }
            context.refreshAll();
        } catch (Exception exception) {
            UiKit.error("No se pudo registrar el pedido", exception);
        }
    }

    private void configureTable() {
        TableColumn<Order, String> detail = wrappingColumn("Detalle", this::detail);
        table.getColumns().addAll(
                column("Fecha", order -> UiFormat.dateTime(order.getCreatedAt())),
                column("Condición", order -> UiFormat.label(order.getPaymentCondition())),
                detail,
                column("Total", order -> UiFormat.money(order.getTotalCost())),
                column("Estado", this::orderStatusLabel)
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Aún no hay pedidos"));
    }

    private void beginCorrection() {
        Order selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiKit.error("Selecciona un pedido", "Debes seleccionar el pedido que quieres corregir");
            return;
        }
        if (selected.getStatus() != OrderStatus.ACTIVE) {
            UiKit.error("Pedido no editable", "Solo pueden corregirse pedidos activos");
            return;
        }
        correctedOrderId = selected.getId();
        condition.getSelectionModel().select(selected.getPaymentCondition());
        for (TextField field : quantities.values()) field.setText("0");
        for (OrderItem item : selected.getItems()) {
            TextField quantity = quantities.get(item.getGuideId());
            TextField cost = costs.get(item.getGuideId());
            if (quantity != null) quantity.setText(String.valueOf(item.getQuantity()));
            if (cost != null) cost.setText(item.getUnitCost().toPlainString());
        }
        submit.setText("Guardar corrección");
        cancelCorrection.setVisible(true);
        cancelCorrection.setManaged(true);
        correctionMode.setText("Corrigiendo pedido #" + selected.getId()
                + ". Cambia cantidades, costos o condición y guarda. Stock solo cambiará por la diferencia.");
        correctionMode.setVisible(true);
        correctionMode.setManaged(true);
    }

    private String orderStatusLabel(Order order) {
        if (order.getStatus() == OrderStatus.ACTIVE && order.hasPendingCost()) return "Costo pendiente";
        return UiFormat.label(order.getStatus());
    }

    private void finishCorrection() {
        correctedOrderId = null;
        submit.setText("Registrar pedido");
        cancelCorrection.setVisible(false);
        cancelCorrection.setManaged(false);
        correctionMode.setVisible(false);
        correctionMode.setManaged(false);
    }

    private void cancelSelectedOrder() {
        Order selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiKit.error("Selecciona un pedido", "Debes seleccionar el pedido que quieres anular");
            return;
        }
        if (selected.getStatus() != OrderStatus.ACTIVE) {
            UiKit.error("Pedido no anulable", "Pedido ya fue corregido o anulado");
            return;
        }
        UiKit.reason("Anular pedido", "Motivo obligatorio:").ifPresent(reason -> {
            if (!UiKit.confirm("Confirmar anulación",
                    "Se descontarán guías recibidas y se recalculará deuda. Registro permanecerá en historial.")) return;
            try {
                context.orderTransactions.cancelOrder(
                        selected.getId(), reason, LocalDateTime.now());
                if (correctedOrderId != null && correctedOrderId == selected.getId()) finishCorrection();
                UiKit.info("Pedido anulado", "Stock y deuda fueron actualizados");
                context.refreshAll();
            } catch (Exception exception) {
                UiKit.error("No se pudo anular el pedido", exception);
            }
        });
    }

    private String detail(Order order) {
        try {
            Map<Long, String> names = context.queries.guideNames();
            return order.getItems().stream().map(item -> item.getQuantity() + "× " + names.getOrDefault(item.getGuideId(), "Guía")).reduce((a, b) -> a + ", " + b).orElse("");
        } catch (Exception exception) {
            return "—";
        }
    }

    private TableColumn<Order, String> column(String title, java.util.function.Function<Order, String> getter) {
        TableColumn<Order, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        return column;
    }

    private TableColumn<Order, String> wrappingColumn(
            String title, java.util.function.Function<Order, String> getter) {
        TableColumn<Order, String> column = column(title, getter);
        column.setCellFactory(ignored -> new TableCell<>() {
            private final Label text = new Label();
            {
                text.setWrapText(true);
                text.setMaxWidth(Double.MAX_VALUE);
                text.maxWidthProperty().bind(widthProperty().subtract(12));
            }
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                text.setText(empty || value == null ? "" : value);
                setGraphic(empty ? null : text);
                setText(null);
            }
        });
        return column;
    }

    @Override
    public void refresh() {
        try {
            rebuildItems(context.guides.findAll());
            ArrayList<Order> orders = new ArrayList<>(context.orders.findAll());
            Collections.reverse(orders);
            table.setItems(FXCollections.observableArrayList(orders));
        } catch (Exception exception) {
            UiKit.error("No se pudieron cargar los pedidos", exception);
        }
    }
}
