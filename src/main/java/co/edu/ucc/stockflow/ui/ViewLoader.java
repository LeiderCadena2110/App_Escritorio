package co.edu.ucc.stockflow.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import javafx.application.HostServices;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/** Loads the .fxml views. Controllers are created by Spring (setControllerFactory). */
@Component
public class ViewLoader {

    /** A loaded view: its root node and its controller. */
    public record LoadedView<T>(Parent root, T controller) {
    }

    private final ApplicationContext context;
    private final ThemeManager theme;
    private HostServices hostServices;

    public ViewLoader(ApplicationContext context, ThemeManager theme) {
        this.context = context;
        this.theme = theme;
    }

    public <T> LoadedView<T> load(String name) {
        URL url = ViewLoader.class.getResource("/views/" + name + ".fxml");
        if (url == null) throw new IllegalStateException("View not found: /views/" + name + ".fxml");
        FXMLLoader loader = new FXMLLoader(url);
        loader.setControllerFactory(context::getBean);
        try {
            Parent root = loader.load();
            return new LoadedView<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load " + name + ".fxml", e);
        }
    }

    public void openMainWindow(Stage stage) {
        LoadedView<MainController> main = load("main");
        Scene scene = new Scene(main.root());
        theme.apply(scene);
        main.controller().start(stage, scene);
    }

    /** Opens a file (PDF, CSV) with the program the system uses for it. */
    public void openFile(java.nio.file.Path file) {
        if (hostServices != null) hostServices.showDocument(file.toUri().toString());
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }
}
