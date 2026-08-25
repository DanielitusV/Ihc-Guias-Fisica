package com.litus.guias;

import com.litus.guias.persistence.ApplicationBootstrap;
import com.litus.guias.persistence.Database;
import com.litus.guias.persistence.GuideRepository;
import com.litus.guias.ui.AppContext;
import com.litus.guias.ui.AppIcon;
import com.litus.guias.ui.MainWindow;
import com.litus.guias.ui.SetupView;
import com.litus.guias.ui.UiKit;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.nio.file.Path;

public final class MainApplication extends Application {
    private Stage stage;
    private ApplicationBootstrap bootstrap;
    private Database database;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.setTitle("Guías Física");
        stage.getIcons().add(AppIcon.image());
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        try {
            String override = System.getProperty("guias.data.dir");
            bootstrap = override == null || override.isBlank()
                    ? new ApplicationBootstrap()
                    : new ApplicationBootstrap(Path.of(override));
            database = bootstrap.initialize();
            if (new GuideRepository(database).findAll().isEmpty()) showSetup();
            else showMain();
            sizeStage();
            stage.show();
        } catch (Exception exception) {
            UiKit.error("No se pudo iniciar Guías Física", exception);
        }
    }

    private void showSetup() {
        SetupView setup = new SetupView(bootstrap, this::showMain);
        setScene(new Scene(setup));
    }

    private void showMain() {
        MainWindow root = new MainWindow(new AppContext(database));
        setScene(new Scene(root));
    }

    private void setScene(Scene scene) {
        var css = MainApplication.class.getResource("/com/litus/guias/ui/aero.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
    }

    private void sizeStage() {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth(Math.min(1400, Math.max(760, bounds.getWidth() * .90)));
        stage.setHeight(Math.min(860, Math.max(560, bounds.getHeight() * .90)));
        stage.setX(bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2);
    }
}
