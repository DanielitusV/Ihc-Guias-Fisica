package com.litus.guias.ui;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Objects;

public final class AppIcon {
    private static final String RESOURCE = "/com/litus/guias/icon/umss.png";

    private AppIcon() {
    }

    public static Image image() {
        return new Image(Objects.requireNonNull(
                AppIcon.class.getResourceAsStream(RESOURCE),
                "No se encontró el icono de la aplicación"
        ));
    }

    public static void apply(Window window) {
        if (window instanceof Stage stage && stage.getIcons().isEmpty()) {
            stage.getIcons().add(image());
        }
    }
}
