package com.litus.guias.ui;

import com.litus.guias.account.AccountMovement;
import com.litus.guias.closure.CashClosure;
import com.litus.guias.order.Order;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.AuthorizedDelivery;
import com.litus.guias.sale.AuthorizedDeliveryReturn;
import com.litus.guias.persistence.AcademicTerm;
import com.litus.guias.inventory.InventoryAdjustment;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.Map;

public final class HistoryView extends VBox implements RefreshableView {
    private final AppContext context;
    private final DatePicker from = new DatePicker(LocalDate.now().minusMonths(3));
    private final DatePicker to = new DatePicker(LocalDate.now());
    private final ComboBox<AcademicTerm> term = new ComboBox<>();
    private final TableView<Sale> sales = new TableView<>();
    private final TableView<Order> orders = new TableView<>();
    private final TableView<AccountMovement> movements = new TableView<>();
    private final TableView<CashClosure> closures = new TableView<>();
    private final TableView<AuthorizedDelivery> deliveries = new TableView<>();
    private final TableView<AuthorizedDeliveryReturn> deliveryReturns = new TableView<>();
    private final TableView<InventoryAdjustment> inventoryAdjustments = new TableView<>();

    public HistoryView(AppContext context) {
        this.context = context;
        getStyleClass().add("page"); setPadding(new Insets(22)); setSpacing(18);
        term.setConverter(new javafx.util.StringConverter<>() {
            public String toString(AcademicTerm value) { return value == null ? "" : value.code(); }
            public AcademicTerm fromString(String value) { return null; }
        });
        var filter = new javafx.scene.control.Button("Aplicar filtros");
        filter.setOnAction(event -> refresh());
        HBox controls = new HBox(10, new Label("Gestión"), term,
                new Label("Desde"), from, new Label("Hasta"), to, filter);
        configureTables();
        TabPane tabs = new TabPane(
                tab("Ventas", sales), tab("Entregas autorizadas", deliveries),
                tab("Devoluciones", deliveryReturns),
                tab("Pedidos", orders), tab("Ajustes de inventario", inventoryAdjustments),
                tab("Movimientos", movements), tab("Cierres", closures));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefHeight(600);
        getChildren().addAll(UiKit.title("Historiales"), controls, tabs);
        context.onRefresh(this::refresh);
        refresh();
    }

    private Tab tab(String title, TableView<?> table) { return new Tab(title, table); }

    private void configureTables() {
        sales.getColumns().addAll(saleColumn("Fecha", s -> UiFormat.dateTime(s.getCreatedAt())), saleColumn("Guía", s -> guideName(s.getGuideId())), saleColumn("Pago", s -> UiFormat.label(s.getPaymentMethod())), saleColumn("Importe", s -> UiFormat.money(s.getPrice())), saleColumn("Estado", s -> UiFormat.label(s.getStatus())), saleColumn("Motivo", s -> s.getCancellationReason() == null ? "" : s.getCancellationReason()));
        orders.getColumns().addAll(orderColumn("Fecha", o -> UiFormat.dateTime(o.getCreatedAt())), orderColumn("Condición", o -> UiFormat.label(o.getPaymentCondition())), orderColumn("Artículos", o -> o.getItems().stream().map(i -> i.getQuantity() + "× " + guideName(i.getGuideId())).reduce((a,b) -> a + ", " + b).orElse("")), orderColumn("Total", o -> UiFormat.money(o.getTotalCost())), orderColumn("Estado", o -> o.getStatus() == com.litus.guias.order.OrderStatus.ACTIVE && o.hasPendingCost() ? "Costo pendiente" : UiFormat.label(o.getStatus())), orderColumn("Motivo", o -> o.getCancellationReason() == null ? "" : o.getCancellationReason()));
        inventoryAdjustments.getColumns().addAll(
                adjustmentColumn("Fecha", value -> UiFormat.dateTime(value.createdAt())),
                adjustmentColumn("Guía", value -> guideName(value.guideId())),
                adjustmentColumn("Tipo", value -> UiFormat.label(value.type())),
                adjustmentColumn("Cambio", value -> (value.quantityDelta() > 0 ? "+" : "") + value.quantityDelta()),
                adjustmentColumn("Motivo", InventoryAdjustment::reason));
        movements.getColumns().addAll(movementColumn("Fecha", m -> UiFormat.dateTime(m.getCreatedAt())), movementColumn("Cuenta", m -> accountName(m.getAccountId())), movementColumn("Tipo", m -> UiFormat.label(m.getType())), movementColumn("Concepto", m -> UiFormat.label(m.getConcept())), movementColumn("Importe", m -> UiFormat.money(m.getAmount())), movementColumn("Motivo", m -> m.getReason() == null ? "" : m.getReason()));
        closures.getColumns().addAll(closureColumn("Fecha", c -> UiFormat.dateTime(c.getCreatedAt())), closureColumn("Efectivo esperado", c -> UiFormat.money(c.getExpectedCash())), closureColumn("Diferencia efectivo", c -> UiFormat.money(c.getCashDifference())), closureColumn("Diferencia QR", c -> UiFormat.money(c.getQrDifference())), closureColumn("Estado", c -> UiFormat.label(c.getStatus())), closureColumn("Notas", c -> c.getNotes() == null ? "" : c.getNotes()));
        deliveries.getColumns().addAll(deliveryColumn("Fecha", d -> UiFormat.dateTime(d.createdAt())), deliveryColumn("Guía", d -> guideName(d.guideId())), deliveryColumn("Cantidad", d -> String.valueOf(d.quantity())), deliveryColumn("Beneficiario", AuthorizedDelivery::beneficiary), deliveryColumn("Autorizó", AuthorizedDelivery::authorizedBy), deliveryColumn("Motivo", AuthorizedDelivery::reason));
        deliveryReturns.getColumns().addAll(
                returnColumn("Fecha", value -> UiFormat.dateTime(value.createdAt())),
                returnColumn("Guía", value -> guideName(value.guideId())),
                returnColumn("Cantidad", value -> String.valueOf(value.quantity())),
                returnColumn("Entrega #", value -> String.valueOf(value.deliveryId())),
                returnColumn("Motivo", AuthorizedDeliveryReturn::reason));
        for (TableView<?> table : new TableView<?>[]{sales, deliveries, deliveryReturns, orders, inventoryAdjustments, movements, closures}) {
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.setPlaceholder(new Label("No existen registros para el rango elegido"));
        }
    }

    private String guideName(long id) { try { return context.queries.guideNames().getOrDefault(id, "—"); } catch (Exception e) { return "—"; } }
    private String accountName(long id) { try { return context.queries.accountNames().getOrDefault(id, "—"); } catch (Exception e) { return "—"; } }
    private TableColumn<Sale,String> saleColumn(String t, java.util.function.Function<Sale,String> f) { TableColumn<Sale,String> c=new TableColumn<>(t); c.setCellValueFactory(d->new ReadOnlyStringWrapper(f.apply(d.getValue()))); return c; }
    private TableColumn<Order,String> orderColumn(String t, java.util.function.Function<Order,String> f) { TableColumn<Order,String> c=new TableColumn<>(t); c.setCellValueFactory(d->new ReadOnlyStringWrapper(f.apply(d.getValue()))); return c; }
    private TableColumn<AccountMovement,String> movementColumn(String t, java.util.function.Function<AccountMovement,String> f) { TableColumn<AccountMovement,String> c=new TableColumn<>(t); c.setCellValueFactory(d->new ReadOnlyStringWrapper(f.apply(d.getValue()))); return c; }
    private TableColumn<CashClosure,String> closureColumn(String t, java.util.function.Function<CashClosure,String> f) { TableColumn<CashClosure,String> c=new TableColumn<>(t); c.setCellValueFactory(d->new ReadOnlyStringWrapper(f.apply(d.getValue()))); return c; }
    private TableColumn<AuthorizedDelivery,String> deliveryColumn(String t, java.util.function.Function<AuthorizedDelivery,String> f) { TableColumn<AuthorizedDelivery,String> c=new TableColumn<>(t); c.setCellValueFactory(d->new ReadOnlyStringWrapper(f.apply(d.getValue()))); return c; }
    private TableColumn<AuthorizedDeliveryReturn,String> returnColumn(String t, java.util.function.Function<AuthorizedDeliveryReturn,String> f) { TableColumn<AuthorizedDeliveryReturn,String> c=new TableColumn<>(t); c.setCellValueFactory(d->new ReadOnlyStringWrapper(f.apply(d.getValue()))); return c; }
    private TableColumn<InventoryAdjustment,String> adjustmentColumn(String t, java.util.function.Function<InventoryAdjustment,String> f) { TableColumn<InventoryAdjustment,String> c=new TableColumn<>(t); c.setCellValueFactory(d->new ReadOnlyStringWrapper(f.apply(d.getValue()))); return c; }

    @Override
    public void refresh() {
        try {
            LocalDate start = from.getValue() == null ? LocalDate.of(2000,1,1) : from.getValue();
            LocalDate end = to.getValue() == null ? LocalDate.now() : to.getValue();
            if (end.isBefore(start)) throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
            var terms = context.terms.findAll();
            AcademicTerm previous = term.getValue();
            term.setItems(FXCollections.observableArrayList(terms));
            if (previous != null) term.getSelectionModel().select(
                    terms.stream().filter(value -> value.id() == previous.id()).findFirst().orElse(null));
            if (term.getValue() == null && !terms.isEmpty()) term.getSelectionModel().selectFirst();
            AcademicTerm selected = term.getValue();
            if (selected == null) return;
            long termId = selected.id();
            sales.setItems(FXCollections.observableArrayList(context.sales.findBetweenByTerm(termId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())));
            deliveries.setItems(FXCollections.observableArrayList(context.authorizedDeliveries.findBetweenByTerm(termId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())));
            deliveryReturns.setItems(FXCollections.observableArrayList(context.authorizedDeliveryReturns.findBetweenByTerm(termId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())));
            orders.setItems(FXCollections.observableArrayList(context.orders.findAllByTerm(termId).stream().filter(o -> !o.getCreatedAt().toLocalDate().isBefore(start) && !o.getCreatedAt().toLocalDate().isAfter(end)).toList()));
            inventoryAdjustments.setItems(FXCollections.observableArrayList(context.inventoryAdjustments.findAllByTerm(termId).stream().filter(value -> !value.createdAt().toLocalDate().isBefore(start) && !value.createdAt().toLocalDate().isAfter(end)).toList()));
            movements.setItems(FXCollections.observableArrayList(context.movements.findBetweenByTerm(termId, start.atStartOfDay(), end.plusDays(1).atStartOfDay())));
            closures.setItems(FXCollections.observableArrayList(context.closures.findAllByTerm(termId).stream().filter(c -> !c.getCreatedAt().toLocalDate().isBefore(start) && !c.getCreatedAt().toLocalDate().isAfter(end)).toList()));
        } catch (Exception exception) { UiKit.error("No se pudieron cargar los historiales", exception); }
    }
}
