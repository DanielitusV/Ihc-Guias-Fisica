package com.litus.guias.ui;

import com.litus.guias.persistence.AcademicTerm;
import com.litus.guias.persistence.AcademicTermStatus;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

public final class ManagementView extends VBox implements RefreshableView {
    private final AppContext context;
    private final Label state = new Label();
    private final TextField newCode = new TextField();
    private final javafx.scene.control.Button close = UiKit.danger("Cerrar gestión activa");
    private final javafx.scene.control.Button open = UiKit.primary("Abrir nueva gestión");
    private final TableView<AcademicTerm> history = new TableView<>();

    public ManagementView(AppContext context) {
        this.context = context;
        getStyleClass().add("page");
        setPadding(new Insets(22));
        setSpacing(18);
        newCode.setPromptText("Ejemplo: 1-2027");
        close.setOnAction(event -> closeTerm());
        open.setOnAction(event -> openTerm());
        configureTable();
        VBox current = UiKit.card("Gestión actual", state, UiKit.actions(close));
        VBox next = UiKit.card("Nueva gestión", new Label(
                "Solo gestiones regulares: 1-AAAA o 2-AAAA. Stock, saldos y deuda empiezan en cero."),
                newCode, UiKit.actions(open));
        history.setPrefHeight(330);
        getChildren().addAll(UiKit.title("Gestiones semestrales"), current, next,
                UiKit.card("Historial de gestiones", history));
        context.onRefresh(this::refresh);
        refresh();
    }

    private void configureTable() {
        history.getColumns().addAll(
                column("Gestión", AcademicTerm::code),
                column("Estado", term -> term.status() == AcademicTermStatus.OPEN ? "Activa" : "Cerrada"),
                column("Apertura", term -> UiFormat.dateTime(term.openedAt())),
                column("Cierre", term -> term.closedAt() == null ? "—" : UiFormat.dateTime(term.closedAt())));
        history.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        history.setPlaceholder(new Label("No hay gestiones registradas"));
    }

    private TableColumn<AcademicTerm, String> column(String title,
            java.util.function.Function<AcademicTerm, String> value) {
        TableColumn<AcademicTerm, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(value.apply(data.getValue())));
        return column;
    }

    private void closeTerm() {
        try {
            AcademicTerm active = context.terms.findActive();
            if (active == null) { UiKit.error("Sin gestión activa", "Primero abre una nueva gestión"); return; }
            String password = askPassword();
            if (password == null) return;
            if (!context.termService.verifyPassword(password)) {
                UiKit.error("Contraseña incorrecta", "No se cerró la gestión");
                return;
            }
            String warning = "Vas a cerrar " + active.code()
                    + ". Sus datos quedarán solo como historial. Stock, Efectivo, QR y deuda se reiniciarán a cero."
                    + " Esta es la segunda y última confirmación.";
            if (!UiKit.confirm("Confirmar cierre definitivo", warning)) return;
            context.termService.closeActive(password, true, LocalDateTime.now());
            UiKit.info("Gestión cerrada", active.code() + " quedó protegida como historial. Ahora abre la siguiente gestión.");
            context.refreshAll();
        } catch (Exception exception) { UiKit.error("No se pudo cerrar la gestión", exception); }
    }

    private String askPassword() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Contraseña de seguridad");
        dialog.setHeaderText("Confirma el cierre de gestión");
        PasswordField field = new PasswordField();
        field.setPromptText("Contraseña");
        VBox content = new VBox(8, new Label("Ingresa la contraseña para continuar:"), field);
        dialog.getDialogPane().setContent(content);
        ButtonType continueButton = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, continueButton);
        dialog.setResultConverter(button -> button == continueButton ? field.getText() : null);
        dialog.setOnShown(event -> {
            var css = UiKit.class.getResource("/com/litus/guias/ui/aero.css");
            if (css != null) dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
            field.requestFocus();
        });
        return dialog.showAndWait().orElse(null);
    }

    private void openTerm() {
        try {
            AcademicTerm term = context.termService.open(newCode.getText(), LocalDateTime.now());
            newCode.clear();
            UiKit.info("Gestión abierta", term.code() + " está lista para trabajar");
            context.refreshAll();
        } catch (Exception exception) { UiKit.error("No se pudo abrir la gestión", exception); }
    }

    @Override public void refresh() {
        try {
            AcademicTerm active = context.terms.findActive();
            state.setText(active == null ? "No existe gestión activa" : active.code() + " · Activa");
            close.setDisable(active == null);
            open.setDisable(active != null);
            history.setItems(FXCollections.observableArrayList(context.terms.findAll()));
        } catch (Exception exception) { UiKit.error("No se pudieron cargar las gestiones", exception); }
    }
}
