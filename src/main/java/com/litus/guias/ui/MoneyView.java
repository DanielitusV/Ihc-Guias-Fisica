package com.litus.guias.ui;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.AccountMovementConcept;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MoneyView extends VBox implements RefreshableView {
    private final AppContext context;
    private final FlowPane metrics = new FlowPane(14, 14);
    private final ComboBox<Account> expenseAccount = accountCombo();
    private final TextField expenseAmount = new TextField();
    private final TextField expenseReason = new TextField();
    private final ComboBox<Account> paymentAccount = accountCombo();
    private final TextField paymentAmount = new TextField();
    private final TextField paymentReason = new TextField();
    private final ComboBox<Account> incomeAccount = accountCombo();
    private final TextField incomeAmount = new TextField();
    private final TextField incomeReason = new TextField();
    private final ComboBox<Account> transferSource = accountCombo();
    private final ComboBox<Account> transferDestination = accountCombo();
    private final TextField transferAmount = new TextField();
    private final TextField transferReason = new TextField();
    private final DatePicker from = new DatePicker(LocalDate.now().minusMonths(1));
    private final DatePicker to = new DatePicker(LocalDate.now());
    private final ComboBox<AccountMovementConcept> concept = new ComboBox<>();
    private final TableView<AccountMovement> table = new TableView<>();

    public MoneyView(AppContext context) {
        this.context = context;
        getStyleClass().addAll("page", "money-page");
        setPadding(new Insets(22));
        setSpacing(18);
        metrics.setPrefWrapLength(900);
        expenseAmount.setPromptText("Importe en Bs");
        expenseReason.setPromptText("Motivo obligatorio");
        paymentAmount.setPromptText("Importe en Bs");
        paymentReason.setPromptText("Referencia o motivo");
        incomeAmount.setPromptText("Importe en Bs");
        incomeReason.setPromptText("Origen o motivo obligatorio");
        transferAmount.setPromptText("Importe en Bs");
        transferReason.setPromptText("Motivo obligatorio");
        var expenseButton = UiKit.primary("Registrar gasto");
        expenseButton.setOnAction(event -> registerExpense());
        var paymentButton = UiKit.primary("Pagar proveedor");
        paymentButton.setOnAction(event -> registerPayment());
        var incomeButton = UiKit.primary("Registrar ingreso");
        incomeButton.setOnAction(event -> registerIncome());
        var transferButton = UiKit.primary("Transferir entre cuentas");
        transferButton.setOnAction(event -> registerTransfer());
        TabPane movementTabs = new TabPane(
                movementTab("Transferencia", movementForm(
                        help("Mueve dinero entre cuentas sin crear un ingreso ni un gasto nuevo."),
                        new Label("Cuenta de origen"), transferSource,
                        new Label("Cuenta de destino"), transferDestination,
                        transferAmount, transferReason, UiKit.actions(transferButton))),
                movementTab("Ingreso", movementForm(
                        help("Registra sobrantes u otros ingresos manuales."),
                        new Label("Cuenta"), incomeAccount,
                        incomeAmount, incomeReason, UiKit.actions(incomeButton))),
                movementTab("Gasto", movementForm(
                        help("Registra un gasto operativo y descuéntalo de la cuenta elegida."),
                        new Label("Cuenta"), expenseAccount,
                        expenseAmount, expenseReason, UiKit.actions(expenseButton))),
                movementTab("Pago proveedor", movementForm(
                        help("Registra un pago y reduce la deuda pendiente con el proveedor."),
                        new Label("Cuenta"), paymentAccount,
                        paymentAmount, paymentReason, UiKit.actions(paymentButton)))
        );
        movementTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        movementTabs.getStyleClass().add("money-movement-tabs");

        concept.getItems().add(null);
        concept.getItems().addAll(AccountMovementConcept.values());
        concept.setConverter(new StringConverter<>() {
            public String toString(AccountMovementConcept value) { return value == null ? "Todos los conceptos" : UiFormat.label(value); }
            public AccountMovementConcept fromString(String value) { return null; }
        });
        concept.getSelectionModel().selectFirst();
        var filter = new javafx.scene.control.Button("Aplicar filtros");
        filter.setOnAction(event -> refreshMovements());
        HBox filters = new HBox(10, new Label("Desde"), from, new Label("Hasta"), to, concept, filter);
        configureTable();
        getChildren().addAll(UiKit.title("Movimientos de dinero"), metrics,
                UiKit.card("Registrar movimiento", movementTabs),
                UiKit.card("Historial de movimientos", filters, table));
        table.setPrefHeight(370);
        context.onRefresh(this::refresh);
        refresh();
    }

    private ComboBox<Account> accountCombo() {
        ComboBox<Account> combo = new ComboBox<>();
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new StringConverter<>() {
            public String toString(Account value) { return value == null ? "" : value.getName(); }
            public Account fromString(String value) { return null; }
        });
        return combo;
    }

    private Tab movementTab(String title, Node content) {
        return new Tab(title, content);
    }

    private VBox movementForm(Node... content) {
        VBox form = new VBox(10);
        form.getStyleClass().add("money-movement-form");
        for (int index = 0; index < content.length; index++) {
            Node node = content[index];
            if (index == content.length - 1 && node.getStyleClass().contains("action-row")) {
                Region spacer = new Region();
                VBox.setVgrow(spacer, Priority.ALWAYS);
                form.getChildren().add(spacer);
            }
            form.getChildren().add(node);
        }
        return form;
    }

    private Label help(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("money-form-help");
        return label;
    }

    private void registerExpense() {
        Account account = expenseAccount.getValue();
        if (account == null) { UiKit.error("Cuenta requerida", "Selecciona la cuenta del gasto"); return; }
        try {
            var amount = UiKit.decimal(expenseAmount.getText(), "El importe");
            String reason = expenseReason.getText().trim();
            if (reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
            if (!UiKit.confirm("Confirmar gasto", UiFormat.money(amount) + " desde " + account.getName())) return;
            context.expenseTransactions.registerExpense(account.getId(), amount, reason, LocalDateTime.now());
            expenseAmount.clear(); expenseReason.clear();
            context.refreshAll();
        } catch (Exception exception) { UiKit.error("No se pudo registrar el gasto", exception); }
    }

    private void registerPayment() {
        Account account = paymentAccount.getValue();
        if (account == null) { UiKit.error("Cuenta requerida", "Selecciona la cuenta para el pago"); return; }
        try {
            var amount = UiKit.decimal(paymentAmount.getText(), "El importe");
            String reason = paymentReason.getText().trim();
            if (reason.isBlank()) reason = "Pago al proveedor";
            if (!UiKit.confirm("Confirmar pago", UiFormat.money(amount) + " desde " + account.getName())) return;
            context.supplierPayments.registerPayment(account.getId(), amount, reason, LocalDateTime.now());
            paymentAmount.clear(); paymentReason.clear();
            context.refreshAll();
        } catch (Exception exception) { UiKit.error("No se pudo registrar el pago", exception); }
    }

    private void registerIncome() {
        Account account = incomeAccount.getValue();
        if (account == null) { UiKit.error("Cuenta requerida", "Selecciona la cuenta del ingreso"); return; }
        try {
            var amount = UiKit.decimal(incomeAmount.getText(), "El importe");
            String reason = incomeReason.getText().trim();
            if (reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
            if (!UiKit.confirm("Confirmar ingreso", UiFormat.money(amount) + " hacia " + account.getName())) return;
            context.manualIncome.register(account.getId(), amount, reason, LocalDateTime.now());
            incomeAmount.clear(); incomeReason.clear();
            context.refreshAll();
        } catch (Exception exception) { UiKit.error("No se pudo registrar el ingreso", exception); }
    }

    private void registerTransfer() {
        Account source = transferSource.getValue();
        Account destination = transferDestination.getValue();
        if (source == null || destination == null) {
            UiKit.error("Cuentas requeridas", "Selecciona cuenta de origen y destino");
            return;
        }
        try {
            if (source.getId() == destination.getId()) {
                throw new IllegalArgumentException("Las cuentas de origen y destino deben ser diferentes");
            }
            var amount = UiKit.decimal(transferAmount.getText(), "El importe");
            String reason = transferReason.getText().trim();
            if (reason.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
            if (!UiKit.confirm("Confirmar transferencia", UiFormat.money(amount) + " de "
                    + source.getName() + " hacia " + destination.getName())) return;
            context.moneyTransfers.transfer(source.getId(), destination.getId(), amount, reason, LocalDateTime.now());
            transferAmount.clear();
            transferReason.clear();
            context.refreshAll();
        } catch (Exception exception) {
            UiKit.error("No se pudo realizar la transferencia", exception);
        }
    }

    private void configureTable() {
        table.getColumns().addAll(
                column("Fecha", movement -> UiFormat.dateTime(movement.getCreatedAt())),
                column("Cuenta", movement -> {
                    try { return context.queries.accountNames().getOrDefault(movement.getAccountId(), "—"); }
                    catch (Exception e) { return "—"; }
                }),
                column("Tipo", movement -> UiFormat.label(movement.getType())),
                column("Concepto", movement -> UiFormat.label(movement.getConcept())),
                column("Importe", movement -> UiFormat.money(movement.getAmount())),
                column("Motivo", movement -> movement.getReason() == null ? "" : movement.getReason())
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No hay movimientos en el rango"));
    }

    private TableColumn<AccountMovement, String> column(String title, java.util.function.Function<AccountMovement, String> getter) {
        TableColumn<AccountMovement, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(getter.apply(data.getValue())));
        return column;
    }

    private void refreshMovements() {
        try {
            LocalDate start = from.getValue() == null ? LocalDate.of(2000, 1, 1) : from.getValue();
            LocalDate end = to.getValue() == null ? LocalDate.now() : to.getValue();
            if (end.isBefore(start)) throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
            List<AccountMovement> values = context.movements.findBetween(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
            if (concept.getValue() != null) values = values.stream().filter(m -> m.getConcept() == concept.getValue()).toList();
            ArrayList<AccountMovement> reversed = new ArrayList<>(values);
            Collections.reverse(reversed);
            table.setItems(FXCollections.observableArrayList(reversed));
        } catch (Exception exception) { UiKit.error("No se pudieron filtrar los movimientos", exception); }
    }

    @Override
    public void refresh() {
        try {
            List<Account> accounts = context.accounts.findAll();
            expenseAccount.setItems(FXCollections.observableArrayList(accounts));
            paymentAccount.setItems(FXCollections.observableArrayList(accounts));
            incomeAccount.setItems(FXCollections.observableArrayList(accounts));
            transferSource.setItems(FXCollections.observableArrayList(accounts));
            transferDestination.setItems(FXCollections.observableArrayList(accounts));
            if (!accounts.isEmpty()) {
                if (expenseAccount.getValue() == null) expenseAccount.setValue(accounts.getFirst());
                if (paymentAccount.getValue() == null) paymentAccount.setValue(accounts.getFirst());
                if (incomeAccount.getValue() == null) incomeAccount.setValue(accounts.getFirst());
                if (transferSource.getValue() == null) transferSource.setValue(accounts.getFirst());
                if (transferDestination.getValue() == null) {
                    transferDestination.setValue(accounts.stream()
                            .filter(account -> account.getName().equals("Cuenta del encargado"))
                            .findFirst().orElse(accounts.getLast()));
                }
            }
            metrics.getChildren().clear();
            for (Account account : accounts) metrics.getChildren().add(UiKit.metric(account.getName(), UiFormat.money(account.getBalance()), "saldo disponible"));
            metrics.getChildren().add(UiKit.metric("Deuda proveedor", UiFormat.money(context.supplierDebt.calculateCurrentDebt()), "pedidos a crédito pendientes"));
            refreshMovements();
        } catch (Exception exception) { UiKit.error("No se pudieron actualizar los movimientos", exception); }
    }
}
