package com.litus.guias.ui;

import com.litus.guias.persistence.ApplicationBootstrap;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SetupView extends StackPane {
    private final ApplicationBootstrap bootstrap;
    private final Runnable completed;
    private final Map<String, TextField> prices = new LinkedHashMap<>();
    private final Map<String, TextField> costs = new LinkedHashMap<>();

    public SetupView(ApplicationBootstrap bootstrap, Runnable completed) {
        this.bootstrap = bootstrap;
        this.completed = completed;
        getStyleClass().add("setup-background");
        setPadding(new Insets(28));
        VBox card = UiKit.card("Configuración inicial");
        card.setMaxWidth(520);
        card.setAlignment(Pos.CENTER_LEFT);
        Label intro = new Label("Define precio de venta y costo unitario. Si aún no conoces un costo, escribe 0. Ambos quedarán guardados.");
        intro.setWrapText(true);
        card.getChildren().add(intro);
        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(9);
        fields.addRow(0, new Label("Guía"), new Label("Precio de venta"), new Label("Costo unitario"));
        int row = 1;
        for (String name : new String[]{"Física General", "Física I", "Física II", "Física III"}) {
            TextField price = new TextField();
            price.setPromptText("Bs");
            TextField cost = new TextField("0");
            cost.setPromptText("Bs");
            prices.put(name, price);
            costs.put(name, cost);
            fields.addRow(row++, new Label(name), price, cost);
        }
        card.getChildren().add(fields);
        var save = UiKit.primary("Guardar y comenzar");
        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(event -> save());
        card.getChildren().add(save);
        getChildren().add(card);
    }

    private void save() {
        try {
            Map<String, BigDecimal> values = new LinkedHashMap<>();
            prices.forEach((name, field) -> values.put(name, UiKit.decimal(field.getText(), "El precio de " + name)));
            Map<String, BigDecimal> unitCosts = new LinkedHashMap<>();
            costs.forEach((name, field) -> unitCosts.put(name,
                    UiKit.nonNegativeDecimal(field.getText(), "El costo de " + name)));
            bootstrap.initializeGuides(values, unitCosts);
            completed.run();
        } catch (Exception exception) {
            UiKit.error("No se pudo completar la configuración", exception);
        }
    }
}
