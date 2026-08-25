package com.litus.guias.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MainWindow extends BorderPane {
    static final String SUPPORT_TEXT = "Soporte\nMaldito Litos\n65318276";
    private final AppContext context;
    private final VBox sidebar = new VBox(7);
    private final Label pageTitle = new Label("Dashboard");
    private final Label dayState = new Label();
    private final Label termState = new Label();
    private final Label pendingReminder = new Label();
    private final Button completePending = new Button("Completar cierre pendiente…");
    private final Button menuButton = new Button("☰");
    private final ScrollPane scroll = new ScrollPane();
    private final Map<String, Node> views = new LinkedHashMap<>();
    private final Map<String, Button> navButtons = new LinkedHashMap<>();
    private ResponsiveMode mode = ResponsiveMode.WIDE;
    private boolean compactMenuOpen;
    private ClosureView closureView;

    private static final Map<String, String> ICONS = Map.of(
            "Dashboard", "⌂", "Ventas", "$", "Inventario", "▦", "Pedidos", "□",
            "Movimientos", "¤", "Cierre", "✓", "Historiales", "≡", "Gestiones", "⚙"
    );

    public MainWindow(AppContext context) {
        this.context = context;
        getStyleClass().add("app-root");
        buildViews();
        buildSidebar();
        setTop(buildHeader());
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.getStyleClass().add("content-scroll");
        setCenter(scroll);
        widthProperty().addListener((obs, old, value) -> applyMode(ResponsiveMode.forWidth(value.doubleValue())));
        context.onRefresh(this::refreshHeader);
        showView("Dashboard");
        applyMode(ResponsiveMode.WIDE);
        refreshHeader();
    }

    private void buildViews() {
        views.put("Dashboard", new DashboardView(context, this::showView));
        views.put("Ventas", new SalesView(context));
        views.put("Inventario", new InventoryView(context));
        views.put("Pedidos", new OrdersView(context));
        views.put("Movimientos", new MoneyView(context));
        closureView = new ClosureView(context);
        views.put("Cierre", closureView);
        views.put("Historiales", new HistoryView(context));
        views.put("Gestiones", new ManagementView(context));
    }

    private void buildSidebar() {
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(18, 10, 16, 10));
        Label brand = new Label("GUÍAS FÍSICA");
        brand.getStyleClass().add("brand");
        sidebar.getChildren().add(brand);
        for (String name : views.keySet()) {
            Button button = new Button(ICONS.get(name) + "  " + name);
            button.getStyleClass().add("nav-button");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setOnAction(event -> {
                showView(name);
                if (mode == ResponsiveMode.COMPACT) { compactMenuOpen = false; setLeft(null); }
            });
            navButtons.put(name, button);
            sidebar.getChildren().add(button);
        }
        Region supportSpacer = new Region();
        VBox.setVgrow(supportSpacer, Priority.ALWAYS);
        Label support = new Label(SUPPORT_TEXT);
        support.setWrapText(true);
        support.setMaxWidth(Double.MAX_VALUE);
        support.getStyleClass().add("sidebar-support");
        sidebar.getChildren().addAll(supportSpacer, support);
        VBox.setVgrow(sidebar, Priority.ALWAYS);
        setLeft(sidebar);
    }

    private Node buildHeader() {
        menuButton.getStyleClass().add("menu-button");
        menuButton.setOnAction(event -> {
            compactMenuOpen = !compactMenuOpen;
            setLeft(compactMenuOpen ? sidebar : null);
        });
        pageTitle.getStyleClass().add("header-title");
        dayState.getStyleClass().add("day-state");
        termState.getStyleClass().add("day-state");
        Label date = new Label(UiFormat.date(LocalDate.now()));
        date.getStyleClass().add("header-date");
        HBox header = new HBox(14, menuButton, pageTitle, UiKit.spacer(), termState,
                date, dayState);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 18, 12, 18));
        header.getStyleClass().add("aero-header");
        pendingReminder.getStyleClass().add("pending-closure-text");
        completePending.getStyleClass().add("pending-closure-button");
        completePending.setOnAction(event -> openFirstPendingClosure());
        HBox reminder = new HBox(12, pendingReminder, UiKit.spacer(), completePending);
        reminder.setAlignment(Pos.CENTER_LEFT);
        reminder.setPadding(new Insets(8, 18, 8, 18));
        reminder.getStyleClass().add("pending-closure-banner");
        reminder.visibleProperty().bind(reminder.managedProperty());
        reminder.setManaged(false);
        VBox top = new VBox(header, reminder);
        top.getProperties().put("closure-reminder", reminder);
        return top;
    }

    public void showView(String name) {
        Node view = views.get(name);
        if (view == null) return;
        pageTitle.setText(name);
        scroll.setContent(view);
        Platform.runLater(() -> scroll.setVvalue(0));
        navButtons.forEach((key, button) -> button.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), key.equals(name)));
        if (view instanceof RefreshableView refreshable) refreshable.refresh();
    }

    private void applyMode(ResponsiveMode newMode) {
        mode = newMode;
        getStyleClass().removeAll("mode-wide", "mode-medium", "mode-compact");
        getStyleClass().add("mode-" + newMode.name().toLowerCase());
        menuButton.setVisible(newMode == ResponsiveMode.COMPACT);
        menuButton.setManaged(newMode == ResponsiveMode.COMPACT);
        if (newMode == ResponsiveMode.WIDE) {
            sidebar.setPrefWidth(220); setLeft(sidebar);
            navButtons.forEach((name, button) -> { button.setText(ICONS.get(name) + "  " + name); button.setAlignment(Pos.CENTER_LEFT); });
        } else if (newMode == ResponsiveMode.MEDIUM) {
            sidebar.setPrefWidth(82); setLeft(sidebar);
            navButtons.forEach((name, button) -> { button.setText(ICONS.get(name)); button.setAlignment(Pos.CENTER); button.setTooltip(new javafx.scene.control.Tooltip(name)); });
        } else {
            sidebar.setPrefWidth(220);
            navButtons.forEach((name, button) -> { button.setText(ICONS.get(name) + "  " + name); button.setAlignment(Pos.CENTER_LEFT); });
            setLeft(compactMenuOpen ? sidebar : null);
        }
    }

    private void refreshHeader() {
        try {
            var active = context.terms.findActive();
            termState.setText(active == null ? "Sin gestión activa" : "Gestión " + active.code());
            dayState.setText(UiFormat.label(context.dayStatus.getStatus(LocalDate.now(), LocalDate.now())));
            var missed = context.dayStatus.findMissedDays(LocalDate.now());
            Node top = getTop();
            if (top instanceof VBox box) {
                Node reminder = (Node) box.getProperties().get("closure-reminder");
                boolean visible = !missed.isEmpty();
                reminder.setManaged(visible);
                pendingReminder.setText(visible
                        ? "⚠ " + missed.size() + (missed.size() == 1 ? " cierre pendiente: " : " cierres pendientes. Primero: ")
                                + UiFormat.date(missed.getFirst())
                        : "");
            }
        } catch (Exception exception) {
            dayState.setText("Estado no disponible");
        }
    }

    private void openFirstPendingClosure() {
        try {
            var missed = context.dayStatus.findMissedDays(LocalDate.now());
            if (missed.isEmpty()) return;
            closureView.selectDate(missed.getFirst());
            showView("Cierre");
        } catch (Exception exception) {
            UiKit.error("No se pudo abrir el cierre pendiente", exception);
        }
    }
}
