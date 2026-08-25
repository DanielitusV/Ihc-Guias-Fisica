package com.litus.guias.ui;

import com.litus.guias.account.Account;
import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.AuthorizedDelivery;
import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.SaleStatus;
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

public final class SalesView extends VBox implements RefreshableView {
    private final AppContext context;
    private final ComboBox<Guide> guide = guideCombo();
    private final ComboBox<PaymentMethod> method = paymentCombo();
    private final Label price = new Label("—");
    private final Label stock = new Label("—");
    private final GridPane multipleGrid = new GridPane();
    private final Map<Long, TextField> multipleQuantities = new LinkedHashMap<>();
    private final ComboBox<PaymentMethod> multipleMethod = paymentCombo();
    private final ComboBox<Guide> authorizedGuide = guideCombo();
    private final TextField authorizedQuantity = new TextField("1");
    private final TextField beneficiary = new TextField();
    private final TextField authorizedBy = new TextField();
    private final TextField authorizationReason = new TextField();
    private final TextField returnQuantity = new TextField();
    private final TextField returnReason = new TextField();
    private final TableView<Sale> table = new TableView<>();
    private final TableView<AuthorizedDelivery> deliveryTable = new TableView<>();
    private final Map<Long, Integer> returnedByDelivery = new LinkedHashMap<>();

    public SalesView(AppContext context) {
        this.context = context;
        getStyleClass().addAll("page", "sales-page");
        setPadding(new Insets(22));
        setSpacing(18);
        method.getSelectionModel().selectFirst();
        multipleMethod.getSelectionModel().selectFirst();
        guide.valueProperty().addListener((obs, old, value) -> updateGuideInfo(value));
        multipleGrid.setHgap(12);
        multipleGrid.setVgap(9);

        Button register = UiKit.primary("Registrar venta simple");
        register.setOnAction(event -> registerSale());
        HBox info = new HBox(18, new Label("Precio:"), price, new Label("Stock:"), stock);
        VBox simple = UiKit.card("Venta simple", new Label("Guía"), guide,
                new Label("Método de pago"), method, info, UiKit.actions(register));

        Button registerMultiple = UiKit.primary("Registrar ventas múltiples");
        registerMultiple.setOnAction(event -> registerMultipleSales());
        VBox multiple = UiKit.card("Ventas múltiples",
                new Label("Indica cantidades. Toda la operación usa el mismo método de pago."),
                multipleGrid, new Label("Método de pago"), multipleMethod,
                UiKit.actions(registerMultiple));
        simple.setPrefWidth(390);
        multiple.setPrefWidth(610);
        FlowPane saleForms = new FlowPane(14, 14, simple, multiple);

        beneficiary.setPromptText("Nombre del beneficiario");
        authorizedBy.setPromptText("Persona que autorizó");
        authorizationReason.setPromptText("Motivo obligatorio");
        authorizedQuantity.setPromptText("Cantidad");
        Button registerDelivery = UiKit.primary("Registrar entrega autorizada");
        registerDelivery.setOnAction(event -> registerAuthorizedDelivery());
        VBox authorized = UiKit.card("Entrega autorizada sin cobro",
                new Label("Descuenta stock y deja constancia; no genera ingreso."),
                new Label("Guía"), authorizedGuide,
                new Label("Cantidad"), authorizedQuantity,
                beneficiary, authorizedBy, authorizationReason,
                UiKit.actions(registerDelivery));

        configureSalesTable();
        configureDeliveryTable();
        returnQuantity.setPromptText("Cantidad devuelta");
        returnQuantity.setPrefColumnCount(8);
        returnReason.setPromptText("Motivo de devolución");
        returnReason.setPrefColumnCount(24);
        Button registerReturn = UiKit.primary("Registrar devolución");
        registerReturn.setOnAction(event -> registerAuthorizedReturn());
        FlowPane returnControls = new FlowPane(10, 8,
                returnQuantity, returnReason, registerReturn);
        returnControls.getStyleClass().add("authorized-return-controls");
        deliveryTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) returnQuantity.setText(String.valueOf(pendingQuantity(selected)));
        });
        Button cancel = UiKit.danger("Anular venta seleccionada");
        cancel.setOnAction(event -> cancelSale());
        table.setPrefHeight(360);
        deliveryTable.setPrefHeight(220);
        getChildren().addAll(
                UiKit.title("Ventas"), saleForms, authorized,
                UiKit.card("Ventas recientes", table, UiKit.actions(cancel)),
                UiKit.card("Entregas autorizadas recientes",
                        new Label("Selecciona una entrega para registrar su devolución parcial o total."),
                        deliveryTable, returnControls));
        context.onRefresh(this::refresh);
        refresh();
    }

    private ComboBox<Guide> guideCombo() {
        ComboBox<Guide> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new StringConverter<>() {
            public String toString(Guide value) { return value == null ? "" : value.getName(); }
            public Guide fromString(String value) { return null; }
        });
        return combo;
    }

    private ComboBox<PaymentMethod> paymentCombo() {
        ComboBox<PaymentMethod> combo = new ComboBox<>();
        combo.setItems(FXCollections.observableArrayList(PaymentMethod.CASH, PaymentMethod.QR));
        combo.setConverter(new StringConverter<>() {
            public String toString(PaymentMethod value) { return UiFormat.label(value); }
            public PaymentMethod fromString(String value) { return null; }
        });
        return combo;
    }

    static Map<Long, Integer> parseMultipleQuantities(Map<Long, String> values) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        try {
            for (var entry : values.entrySet()) {
                int quantity = Integer.parseInt(entry.getValue().trim());
                if (quantity < 0) throw new NumberFormatException();
                if (quantity > 0) quantities.put(entry.getKey(), quantity);
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Las cantidades deben ser números enteros desde cero");
        }
        if (quantities.isEmpty()) throw new IllegalArgumentException("Añade al menos una guía con cantidad mayor a cero");
        return quantities;
    }

    private void registerSale() {
        Guide selected = guide.getValue();
        PaymentMethod payment = method.getValue();
        if (selected == null || payment == null) {
            UiKit.error("Datos incompletos", "Selecciona guía y método de pago");
            return;
        }
        if (!UiKit.confirm("Confirmar venta", selected.getName() + " por "
                + UiFormat.money(selected.getCurrentPrice()) + " mediante " + UiFormat.label(payment))) return;
        try {
            Account account = accountFor(payment);
            context.saleTransactions.registerSale(selected.getId(), account.getId(), payment, LocalDateTime.now());
            UiKit.info("Venta registrada", "La venta se guardó correctamente");
            context.refreshAll();
        } catch (Exception exception) {
            UiKit.error("No se pudo registrar la venta", exception);
        }
    }

    private void registerMultipleSales() {
        try {
            Map<Long, String> raw = new LinkedHashMap<>();
            multipleQuantities.forEach((id, field) -> raw.put(id, field.getText()));
            Map<Long, Integer> quantities = parseMultipleQuantities(raw);
            PaymentMethod payment = multipleMethod.getValue();
            if (payment == null) throw new IllegalArgumentException("Selecciona el método de pago");
            Map<Long, Guide> guides = new LinkedHashMap<>();
            context.guides.findAll().forEach(value -> guides.put(value.getId(), value));
            int units = quantities.values().stream().mapToInt(Integer::intValue).sum();
            BigDecimal total = quantities.entrySet().stream()
                    .map(entry -> guides.get(entry.getKey()).getCurrentPrice()
                            .multiply(BigDecimal.valueOf(entry.getValue())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!UiKit.confirm("Confirmar ventas múltiples", units + " unidades por "
                    + UiFormat.money(total) + " mediante " + UiFormat.label(payment))) return;
            Account account = accountFor(payment);
            context.saleTransactions.registerSales(quantities, account.getId(), payment, LocalDateTime.now());
            UiKit.info("Ventas registradas", units + " unidades registradas correctamente");
            context.refreshAll();
        } catch (Exception exception) {
            UiKit.error("No se pudieron registrar las ventas múltiples", exception);
        }
    }

    private void registerAuthorizedDelivery() {
        Guide selected = authorizedGuide.getValue();
        if (selected == null) {
            UiKit.error("Guía requerida", "Selecciona la guía entregada");
            return;
        }
        try {
            int quantity = UiKit.nonNegativeInt(authorizedQuantity.getText(), "La cantidad");
            if (quantity == 0) throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
            String recipient = beneficiary.getText().trim();
            String authorizer = authorizedBy.getText().trim();
            String reason = authorizationReason.getText().trim();
            if (recipient.isBlank()) throw new IllegalArgumentException("El beneficiario es obligatorio");
            if (authorizer.isBlank()) throw new IllegalArgumentException("La persona que autorizó es obligatoria");
            if (reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
            if (!UiKit.confirm("Confirmar entrega sin cobro",
                    quantity + " × " + selected.getName() + " para " + recipient
                            + ". No se registrará ingreso.")) return;
            context.authorizedDeliveryTransactions.register(
                    selected.getId(), quantity, recipient, authorizer, reason, LocalDateTime.now());
            beneficiary.clear();
            authorizedBy.clear();
            authorizationReason.clear();
            authorizedQuantity.setText("1");
            UiKit.info("Entrega registrada", "El stock y el historial fueron actualizados");
            context.refreshAll();
        } catch (Exception exception) {
            UiKit.error("No se pudo registrar la entrega autorizada", exception);
        }
    }

    private void registerAuthorizedReturn() {
        AuthorizedDelivery selected = deliveryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiKit.error("Selecciona una entrega", "Debes seleccionar la entrega que están devolviendo");
            return;
        }
        try {
            int quantity = UiKit.nonNegativeInt(returnQuantity.getText(), "La cantidad devuelta");
            if (quantity == 0) throw new IllegalArgumentException("La cantidad devuelta debe ser mayor a cero");
            String reason = returnReason.getText().trim();
            if (reason.isBlank()) throw new IllegalArgumentException("El motivo de devolución es obligatorio");
            int pending = pendingQuantity(selected);
            if (quantity > pending) {
                throw new IllegalArgumentException("Solo quedan " + pending + " unidades pendientes de devolución");
            }
            if (!UiKit.confirm("Confirmar devolución autorizada",
                    quantity + " × " + guideName(selected.guideId())
                            + ". El stock aumentará y no se moverá dinero.")) return;
            context.authorizedDeliveryReturnTransactions.register(
                    selected.id(), quantity, reason, LocalDateTime.now());
            returnQuantity.clear();
            returnReason.clear();
            UiKit.info("Devolución registrada", "El stock y el historial fueron actualizados");
            context.refreshAll();
        } catch (Exception exception) {
            UiKit.error("No se pudo registrar la devolución", exception);
        }
    }

    private int pendingQuantity(AuthorizedDelivery delivery) {
        return Math.max(0, delivery.quantity() - returnedByDelivery.getOrDefault(delivery.id(), 0));
    }

    private Account accountFor(PaymentMethod payment) throws Exception {
        return context.accounts.findByName(payment == PaymentMethod.CASH ? "Efectivo" : "QR / Soto");
    }

    private void cancelSale() {
        Sale selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            UiKit.error("Selecciona una venta", "Debes seleccionar una fila activa");
            return;
        }
        if (selected.getStatus() == SaleStatus.CANCELLED) {
            UiKit.error("Venta ya anulada", "Esta venta ya fue anulada");
            return;
        }
        UiKit.reason("Anular venta", "Motivo obligatorio:").ifPresent(reason -> {
            if (!UiKit.confirm("Confirmar anulación", "Se devolverá una unidad al stock y se revertirá el saldo.")) return;
            try {
                context.saleTransactions.cancelSale(selected.getId(), reason, LocalDateTime.now());
                context.refreshAll();
            } catch (Exception exception) {
                UiKit.error("No se pudo anular la venta", exception);
            }
        });
    }

    private void configureSalesTable() {
        table.getColumns().addAll(
                saleColumn("Fecha", sale -> UiFormat.dateTime(sale.getCreatedAt())),
                saleColumn("Guía", sale -> guideName(sale.getGuideId())),
                saleColumn("Pago", sale -> UiFormat.label(sale.getPaymentMethod())),
                saleColumn("Importe", sale -> UiFormat.money(sale.getPrice())),
                saleColumn("Estado", sale -> UiFormat.label(sale.getStatus())),
                saleColumn("Motivo", sale -> sale.getCancellationReason() == null ? "" : sale.getCancellationReason()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Aún no hay ventas"));
    }

    private void configureDeliveryTable() {
        deliveryTable.getColumns().addAll(
                deliveryColumn("Fecha", value -> UiFormat.dateTime(value.createdAt())),
                deliveryColumn("Guía", value -> guideName(value.guideId())),
                deliveryColumn("Entregadas", value -> String.valueOf(value.quantity())),
                deliveryColumn("Devueltas", value -> String.valueOf(
                        returnedByDelivery.getOrDefault(value.id(), 0))),
                deliveryColumn("Pendientes", value -> String.valueOf(pendingQuantity(value))),
                deliveryColumn("Beneficiario", AuthorizedDelivery::beneficiary),
                deliveryColumn("Autorizó", AuthorizedDelivery::authorizedBy),
                deliveryColumn("Motivo", AuthorizedDelivery::reason));
        deliveryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        deliveryTable.setPlaceholder(new Label("Aún no hay entregas autorizadas"));
    }

    private String guideName(long guideId) {
        try { return context.queries.guideNames().getOrDefault(guideId, "—"); }
        catch (Exception exception) { return "—"; }
    }

    private TableColumn<Sale, String> saleColumn(String title, java.util.function.Function<Sale, String> getter) {
        TableColumn<Sale, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        return column;
    }

    private TableColumn<AuthorizedDelivery, String> deliveryColumn(String title,
            java.util.function.Function<AuthorizedDelivery, String> getter) {
        TableColumn<AuthorizedDelivery, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        return column;
    }

    private void rebuildMultipleGrid(List<Guide> guides) {
        multipleGrid.getChildren().clear();
        multipleQuantities.clear();
        multipleGrid.addRow(0, header("Guía"), header("Stock"), header("Cantidad"));
        int row = 1;
        for (Guide value : guides) {
            TextField quantity = new TextField("0");
            quantity.setPrefColumnCount(6);
            multipleQuantities.put(value.getId(), quantity);
            multipleGrid.addRow(row++, new Label(value.getName()),
                    new Label(String.valueOf(value.getStock())), quantity);
        }
    }

    private Label header(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private void updateGuideInfo(Guide value) {
        price.setText(value == null ? "—" : UiFormat.money(value.getCurrentPrice()));
        stock.setText(value == null ? "—" : String.valueOf(value.getStock()));
    }

    private Guide preserveSelection(ComboBox<Guide> combo, List<Guide> guides) {
        Long selectedId = combo.getValue() == null ? null : combo.getValue().getId();
        combo.setItems(FXCollections.observableArrayList(guides));
        Guide selected = guides.stream().filter(value -> selectedId != null && value.getId() == selectedId)
                .findFirst().orElse(guides.isEmpty() ? null : guides.getFirst());
        combo.setValue(selected);
        return selected;
    }

    @Override
    public void refresh() {
        try {
            List<Guide> guides = context.guides.findAll();
            updateGuideInfo(preserveSelection(guide, guides));
            preserveSelection(authorizedGuide, guides);
            rebuildMultipleGrid(guides);
            ArrayList<Sale> sales = new ArrayList<>(context.sales.findAll());
            Collections.reverse(sales);
            table.setItems(FXCollections.observableArrayList(sales));
            ArrayList<AuthorizedDelivery> deliveries = new ArrayList<>(context.authorizedDeliveries.findAll());
            returnedByDelivery.clear();
            context.authorizedDeliveryReturns.findAll().forEach(value ->
                    returnedByDelivery.merge(value.deliveryId(), value.quantity(), Integer::sum));
            Collections.reverse(deliveries);
            deliveryTable.setItems(FXCollections.observableArrayList(deliveries));
        } catch (Exception exception) {
            UiKit.error("No se pudieron cargar las ventas", exception);
        }
    }
}
