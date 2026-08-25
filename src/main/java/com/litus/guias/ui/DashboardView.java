package com.litus.guias.ui;

import com.litus.guias.inventory.Guide;
import com.litus.guias.sale.PaymentMethod;
import com.litus.guias.sale.Sale;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class DashboardView extends VBox implements RefreshableView {
    static final long SAFE_MODE_LOCK_MILLIS = 1_000;
    static final String QUICK_SALE_NOTICE =
            "Las ventas rápidas sin stock permanecen bloqueadas. "
                    + "Actualice el inventario para habilitarlas.";

    private final AppContext context;
    private final Consumer<String> navigate;
    private final QuickSaleService quickSaleService;
    private final GridPane quickSales = new GridPane();
    private final Label quickStatus = new Label("Listo para vender");
    private final Button undoQuickSale = new Button("↶ Deshacer última venta rápida");
    private final ToggleButton safeMode = new ToggleButton();
    private final Label safeModeState = new Label(safeModeStateLabel(true));
    private final PauseTransition safeModeLock = new PauseTransition(Duration.millis(SAFE_MODE_LOCK_MILLIS));
    private final List<Button> quickSaleButtons = new ArrayList<>();
    private final FlowPane metrics = new FlowPane(14, 14);
    private final FlowPane stock = new FlowPane(14, 14);
    private final GridPane weekly = new GridPane();
    private final TableView<Sale> recent = new TableView<>();
    private List<Guide> currentGuides = List.of();
    private Long lastQuickSaleId;
    private int quickColumns;
    private boolean quickSalesLocked;

    public DashboardView(AppContext context, Consumer<String> navigate) {
        this.context = context;
        this.navigate = navigate;
        quickSaleService = new QuickSaleService(context.database);
        getStyleClass().add("page");
        setSpacing(18);
        setPadding(new Insets(22));
        metrics.setPrefWrapLength(900);
        stock.setPrefWrapLength(900);
        quickSales.setHgap(10);
        quickSales.setVgap(10);
        weekly.setHgap(12);
        weekly.setVgap(8);
        weekly.getStyleClass().add("weekly-grid");
        quickSales.widthProperty().addListener((observable, oldValue, newValue) ->
                layoutQuickSales(newValue.doubleValue())
        );
        quickStatus.getStyleClass().add("quick-sale-status");
        quickStatus.setWrapText(true);
        undoQuickSale.getStyleClass().add("quick-undo-button");
        undoQuickSale.setDisable(true);
        undoQuickSale.setOnAction(event -> undoLastQuickSale());
        safeMode.setSelected(true);
        safeMode.getStyleClass().add("safe-mode-toggle");
        StackPane safeModeTrack = new StackPane();
        safeModeTrack.getStyleClass().add("safe-mode-track");
        Circle safeModeKnob = new Circle(9);
        safeModeKnob.getStyleClass().add("safe-mode-knob");
        safeModeTrack.getChildren().add(safeModeKnob);
        safeMode.setGraphic(safeModeTrack);
        safeMode.setAccessibleText("Modo seguro");
        safeMode.setTooltip(new Tooltip("Bloquea las ventas rápidas durante 1 segundo para evitar doble clic"));
        safeModeLock.setOnFinished(event -> unlockQuickSales());
        updateSafeModeSwitch(safeModeKnob, true);
        safeMode.selectedProperty().addListener((obs, old, selected) -> {
            safeModeState.setText(safeModeStateLabel(selected));
            updateSafeModeSwitch(safeModeKnob, selected);
        });
        configureTable();

        Button sell = UiKit.primary("＋ Nueva venta");
        sell.setOnAction(event -> navigate.accept("Ventas"));
        Button order = new Button("📦 Registrar pedido");
        order.setOnAction(event -> navigate.accept("Pedidos"));
        Button closure = new Button("✓ Cerrar jornada");
        closure.setOnAction(event -> navigate.accept("Cierre"));

        HBox quick = new HBox(10, sell, order, closure);
        Label stockNotice = new Label(QUICK_SALE_NOTICE);
        stockNotice.getStyleClass().add("quick-sale-note");
        stockNotice.setMaxWidth(Double.MAX_VALUE);
        VBox quickSaleCard = UiKit.card(
                "Venta rápida — un clic",
                stockNotice,
                quickSales,
                new FlowPane(12, 8, safeModeControl(), undoQuickSale, quickStatus)
        );
        quickSaleCard.getStyleClass().add("quick-sale-card");
        VBox stockCard = UiKit.card("Inventario actual", stock);
        stockCard.getStyleClass().add("inventory-card");
        VBox weeklyCard = UiKit.card("Ventas por guía", new Label("Comparación semanal basada en semanas de lunes a domingo."), weekly);
        weeklyCard.getStyleClass().add("weekly-card");
        VBox recentCard = UiKit.card("Últimas ventas", recent);
        recent.setPrefHeight(230);
        getChildren().addAll(
                UiKit.title("Resumen de hoy"), quickSaleCard, metrics,
                quick, stockCard, weeklyCard, recentCard
        );
        context.onRefresh(this::refresh);
        refresh();
    }

    private HBox safeModeControl() {
        Label label = new Label("Modo seguro");
        label.getStyleClass().add("safe-mode-label");
        safeModeState.getStyleClass().add("safe-mode-state");
        HBox control = new HBox(8, label, safeMode, safeModeState);
        control.setAlignment(Pos.CENTER_LEFT);
        control.getStyleClass().add("safe-mode-control");
        return control;
    }

    private void updateSafeModeSwitch(Circle knob, boolean selected) {
        knob.setTranslateX(selected ? 11 : -11);
        safeMode.setAccessibleHelp(safeModeStateLabel(selected));
    }

    static String safeModeStateLabel(boolean selected) {
        return selected ? "Activado" : "Desactivado";
    }

    private void layoutQuickSales(double width) {
        int columns = quickSaleColumns(width);
        if (columns == quickColumns && quickSales.getChildren().size() == currentGuides.size() * 2) {
            return;
        }
        quickColumns = columns;
        quickSales.getChildren().clear();
        quickSaleButtons.clear();
        quickSales.getColumnConstraints().clear();
        for (int column = 0; column < columns; column++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / columns);
            constraint.setHgrow(Priority.ALWAYS);
            quickSales.getColumnConstraints().add(constraint);
        }
        for (int index = 0; index < currentGuides.size(); index++) {
            Guide guide = currentGuides.get(index);
            int column = index % columns;
            int row = (index / columns) * 2;
            addQuickButton(guide, PaymentMethod.CASH, column, row);
            addQuickButton(guide, PaymentMethod.QR, column, row + 1);
        }
    }

    private void addQuickButton(Guide guide, PaymentMethod method, int column, int row) {
        Button button = quickButton(guide, method);
        quickSaleButtons.add(button);
        quickSales.add(button, column, row);
    }

    private Button quickButton(Guide guide, PaymentMethod method) {
        String payment = method == PaymentMethod.CASH ? "EFECTIVO" : "QR";
        Button button = new Button(quickSaleLabel(guide, method));
        button.setWrapText(true);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(64);
        button.setUserData(guide);
        button.setDisable(quickSaleDisabled(guide, quickSalesLocked));
        button.getStyleClass().addAll(
                "quick-sale-button",
                guideStyle(guide.getName()),
                method == PaymentMethod.QR ? "quick-sale-qr" : "quick-sale-cash"
        );
        button.setTooltip(new Tooltip(
                guide.getStock() <= 0
                        ? "Sin existencias"
                        : "Vender 1 " + guide.getName() + " por " + payment
        ));
        button.setOnAction(event -> registerQuickSale(guide, method));
        return button;
    }

    static String quickSaleLabel(Guide guide, PaymentMethod method) {
        String payment = method == PaymentMethod.CASH ? "Efectivo" : "QR";
        return guide.getName() + "\n" + payment + " · Stock " + guide.getStock();
    }

    static boolean quickSaleDisabled(Guide guide, boolean locked) {
        return locked || guide.getStock() <= 0;
    }

    static int quickSaleColumns(double width) {
        return width >= 900 ? 4 : width >= 700 ? 2 : 1;
    }

    private String guideStyle(String guideName) {
        return switch (guideName) {
            case "Física General" -> "quick-guide-general";
            case "Física I" -> "quick-guide-i";
            case "Física II" -> "quick-guide-ii";
            default -> "quick-guide-iii";
        };
    }

    private void registerQuickSale(Guide guide, PaymentMethod method) {
        if (quickSalesLocked) return;
        boolean protectedBySafeMode = safeMode.isSelected();
        if (protectedBySafeMode) lockQuickSales();
        try {
            LocalDateTime now = LocalDateTime.now();
            lastQuickSaleId = quickSaleService.register(guide.getId(), method, now);
            undoQuickSale.setDisable(false);
            quickStatus.setText(quickSaleSuccessStatus(guide, method, now, protectedBySafeMode));
            quickStatus.getStyleClass().remove("quick-sale-error");
            quickStatus.getStyleClass().add("quick-sale-success");
            context.refreshAll();
        } catch (Exception exception) {
            quickStatus.setText("No se registró la venta: " + safeMessage(exception));
            quickStatus.getStyleClass().remove("quick-sale-success");
            quickStatus.getStyleClass().add("quick-sale-error");
        }
    }

    static String quickSaleSuccessStatus(
            Guide guide, PaymentMethod method, LocalDateTime time, boolean confirmedInSafeMode) {
        String prefix = confirmedInSafeMode ? "✓ Positivo 08 · " : "✓ ";
        return prefix + guide.getName() + " · " + UiFormat.label(method)
                + " · " + UiFormat.money(guide.getCurrentPrice())
                + " · " + time.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private void undoLastQuickSale() {
        if (lastQuickSaleId == null) return;
        try {
            quickSaleService.undo(lastQuickSaleId, LocalDateTime.now());
            lastQuickSaleId = null;
            undoQuickSale.setDisable(true);
            quickStatus.setText("↶ Última venta rápida anulada; stock y saldo restaurados");
            quickStatus.getStyleClass().remove("quick-sale-error");
            quickStatus.getStyleClass().add("quick-sale-success");
            context.refreshAll();
        } catch (Exception exception) {
            quickStatus.setText("No se pudo deshacer: " + safeMessage(exception));
            quickStatus.getStyleClass().remove("quick-sale-success");
            quickStatus.getStyleClass().add("quick-sale-error");
        }
    }

    private void lockQuickSales() {
        quickSalesLocked = true;
        updateQuickSaleButtons();
        safeModeLock.playFromStart();
    }

    private void unlockQuickSales() {
        quickSalesLocked = false;
        updateQuickSaleButtons();
    }

    private void updateQuickSaleButtons() {
        for (Button button : quickSaleButtons) {
            Guide guide = (Guide) button.getUserData();
            button.setDisable(quickSaleDisabled(guide, quickSalesLocked));
        }
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private void configureTable() {
        TableColumn<Sale, String> date = column("Fecha", sale -> UiFormat.dateTime(sale.getCreatedAt()));
        TableColumn<Sale, String> guide = column("Guía", sale -> {
            try { return context.queries.guideNames().getOrDefault(sale.getGuideId(), "Guía"); }
            catch (Exception exception) { return "Guía"; }
        });
        TableColumn<Sale, String> method = column("Pago", sale -> UiFormat.label(sale.getPaymentMethod()));
        TableColumn<Sale, String> amount = column("Importe", sale -> UiFormat.money(sale.getPrice()));
        TableColumn<Sale, String> status = column("Estado", sale -> UiFormat.label(sale.getStatus()));
        recent.getColumns().addAll(date, guide, method, amount, status);
        recent.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        recent.setPlaceholder(new Label("Aún no hay ventas"));
    }

    private TableColumn<Sale, String> column(String name, java.util.function.Function<Sale, String> value) {
        TableColumn<Sale, String> column = new TableColumn<>(name);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(value.apply(data.getValue())));
        return column;
    }

    @Override
    public void refresh() {
        try {
            AppQueries.TodaySummary summary = context.queries.todaySummary(LocalDate.now());
            metrics.getChildren().setAll(
                    UiKit.metric("Ventas activas", String.valueOf(summary.activeSales()), "registradas hoy"),
                    UiKit.metric("Ingresos de hoy", UiFormat.money(summary.income()), "ventas no anuladas"),
                    UiKit.metric("Efectivo", UiFormat.money(summary.cashBalance()), "saldo actual"),
                    UiKit.metric("QR / Soto", UiFormat.money(summary.qrBalance()), "saldo actual"),
                    UiKit.metric("Deuda proveedor", UiFormat.money(summary.supplierDebt()), "pedidos a crédito"),
                    UiKit.metric("Jornada", UiFormat.label(summary.status()), UiFormat.date(LocalDate.now()))
            );
            stock.getChildren().clear();
            currentGuides = context.guides.findAll();
            quickColumns = 0;
            layoutQuickSales(quickSales.getWidth());
            for (Guide guide : currentGuides) {
                Label count = new Label(guide.getStock() + " unidades");
                count.getStyleClass().add(guide.getStock() <= 5 ? "stock-low" : "stock-ok");
                stock.getChildren().add(UiKit.card(guide.getName(), count, new Label(UiFormat.money(guide.getCurrentPrice()))));
            }
            rebuildWeekly(context.queries.weeklyGuideSummary(LocalDate.now()));
            ArrayList<Sale> sales = new ArrayList<>(context.sales.findAll());
            Collections.reverse(sales);
            recent.setItems(FXCollections.observableArrayList(sales.stream().limit(8).toList()));
        } catch (Exception exception) {
            UiKit.error("No se pudo actualizar el dashboard", exception);
        }
    }


    private void rebuildWeekly(List<AppQueries.GuideWeekSummary> summaries) {
        weekly.getChildren().clear();
        weekly.getColumnConstraints().clear();
        for (int column = 0; column < 3; column++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(column == 0 ? 50 : 25);
            constraint.setHgrow(Priority.ALWAYS);
            weekly.getColumnConstraints().add(constraint);
        }
        weekly.add(weeklyHeader("Guía"), 0, 0);
        weekly.add(weeklyHeader("Esta semana"), 1, 0);
        weekly.add(weeklyHeader("Semana anterior"), 2, 0);
        int row = 1;
        for (AppQueries.GuideWeekSummary summary : summaries) {
            Label guide = new Label(summary.guideName());
            guide.getStyleClass().addAll("weekly-guide", guideStyle(summary.guideName()));
            Label current = weeklyValue(summary.currentWeek());
            Label previous = weeklyValue(summary.previousWeek());
            weekly.add(guide, 0, row);
            weekly.add(current, 1, row);
            weekly.add(previous, 2, row++);
        }
    }

    private Label weeklyHeader(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("weekly-header");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label weeklyValue(long value) {
        Label label = new Label(value + (value == 1 ? " venta" : " ventas"));
        label.getStyleClass().add("weekly-value");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }
}
