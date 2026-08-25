package com.litus.guias.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Optional;

public final class UiKit {
    private UiKit() {
    }

    public static Label title(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.getStyleClass().add("page-title");
        return label;
    }

    public static VBox card(String heading, Node... content) {
        Label title = new Label(heading);
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        title.getStyleClass().add("card-title");
        VBox box = new VBox(12, title);
        box.getChildren().addAll(content);
        if (content.length > 0 && content[content.length - 1].getStyleClass().contains("action-row")) {
            Region actionSpacer = new Region();
            actionSpacer.getStyleClass().add("action-spacer");
            VBox.setVgrow(actionSpacer, Priority.ALWAYS);
            box.getChildren().add(box.getChildren().size() - 1, actionSpacer);
        }
        box.getStyleClass().add("aero-card");
        box.setFillWidth(true);
        return box;
    }

    public static VBox metric(String heading, String value, String detail) {
        Label headingLabel = new Label(heading);
        headingLabel.setWrapText(true);
        headingLabel.getStyleClass().add("metric-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("metric-value");
        Label detailLabel = new Label(detail);
        detailLabel.setWrapText(true);
        detailLabel.getStyleClass().add("muted");
        VBox box = new VBox(5, headingLabel, valueLabel, detailLabel);
        box.getStyleClass().addAll("aero-card", "metric-card");
        box.setMinWidth(180);
        return box;
    }

    public static Button primary(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("primary-button");
        return button;
    }

    public static Button danger(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("danger-button");
        return button;
    }

    public static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }

    public static VBox page(Node... nodes) {
        VBox box = new VBox(18, nodes);
        box.setPadding(new Insets(22));
        box.getStyleClass().add("page");
        return box;
    }

    public static HBox actions(Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.getStyleClass().add("action-row");
        for (Node node : nodes) {
            if (node instanceof ButtonBase button && !button.getStyleClass().contains("action-button")) {
                button.getStyleClass().add("action-button");
            }
        }
        return box;
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        style(alert);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static Optional<String> reason(String title, String prompt) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText(prompt);
        style(dialog);
        return dialog.showAndWait().map(String::trim).filter(value -> !value.isBlank());
    }

    public static void info(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message);
    }

    public static void error(String title, Throwable error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
        show(Alert.AlertType.ERROR, title, message);
    }

    public static void error(String title, String message) {
        show(Alert.AlertType.ERROR, title, message);
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        style(alert);
        alert.showAndWait();
    }

    private static void style(javafx.scene.control.Dialog<?> dialog) {
        dialog.setOnShown(event -> {
            var resource = UiKit.class.getResource("/com/litus/guias/ui/aero.css");
            if (resource != null) dialog.getDialogPane().getStylesheets().add(resource.toExternalForm());
            AppIcon.apply(dialog.getDialogPane().getScene().getWindow());
        });
    }

    public static BigDecimal decimal(String text, String field) {
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(',', '.'));
            if (value.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
            return value;
        } catch (Exception exception) {
            throw new IllegalArgumentException(field + " debe ser mayor a cero");
        }
    }

    public static int nonNegativeInt(String text, String field) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 0) throw new NumberFormatException();
            return value;
        } catch (Exception exception) {
            throw new IllegalArgumentException(field + " debe ser un entero no negativo");
        }
    }

    public static BigDecimal nonNegativeDecimal(String text, String field) {
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(',', '.'));
            if (value.signum() < 0) throw new NumberFormatException();
            return value;
        } catch (Exception exception) {
            throw new IllegalArgumentException(field + " debe ser cero o mayor");
        }
    }
}
