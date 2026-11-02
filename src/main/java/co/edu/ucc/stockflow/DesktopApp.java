package co.edu.ucc.stockflow;

import co.edu.ucc.stockflow.service.DataSeeder;
import co.edu.ucc.stockflow.ui.ViewLoader;
import java.io.InputStream;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Starts JavaFX, shows "Iniciando…" while Spring Boot prepares the services and the local
 * database, and then opens the main window.
 */
public class DesktopApp extends Application {

    public static final String NAME = "StockFlow";

    private ConfigurableApplicationContext context;

    @Override
    public void start(Stage stage) {
        stage.setTitle(NAME);
        for (String size : new String[]{"16", "32", "64", "256"}) {
            InputStream in = getClass().getResourceAsStream("/icons/app-icon-" + size + ".png");
            if (in != null) stage.getIcons().add(new Image(in));
        }
        stage.setScene(loadingScene());
        stage.show();

        String[] args = getParameters().getRaw().toArray(String[]::new);
        Task<ConfigurableApplicationContext> startup = new Task<>() {
            @Override
            protected ConfigurableApplicationContext call() {
                // headless(false): lets the app open files and folders with the system explorer
                ConfigurableApplicationContext ctx =
                        new SpringApplicationBuilder(StockFlowApplication.class).headless(false).run(args);
                ctx.getBean(DataSeeder.class).prepare();
                return ctx;
            }
        };
        startup.setOnSucceeded(e -> {
            context = startup.getValue();
            ViewLoader views = context.getBean(ViewLoader.class);
            views.setHostServices(getHostServices());
            views.openMainWindow(stage);
        });
        startup.setOnFailed(e -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(NAME);
            alert.setHeaderText("No se pudo iniciar StockFlow");
            alert.setContentText(rootCause(startup.getException()));
            alert.showAndWait();
            Platform.exit();
        });
        Thread thread = new Thread(startup, "spring-startup");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void stop() {
        if (context != null) context.close();
    }

    private Scene loadingScene() {
        Label title = new Label(NAME);
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #14324f;");
        Label subtitle = new Label("Inventario y facturación");
        Label status = new Label("Iniciando…");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(42, 42);
        VBox box = new VBox(12, title, subtitle, progress, status);
        box.setAlignment(Pos.CENTER);
        return new Scene(box, 420, 260);
    }

    private static String rootCause(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        String message = c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage();
        if (message.contains("already in use") || message.contains("locked by another process")) {
            return "StockFlow ya está abierto en otra ventana. Ciérrela e intente de nuevo.";
        }
        return message;
    }
}
