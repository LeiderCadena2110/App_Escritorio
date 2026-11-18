package co.edu.ucc.stockflow.ui;

import co.edu.ucc.stockflow.service.SettingsService;
import co.edu.ucc.stockflow.shared.ui.ThemeManager;
import co.edu.ucc.stockflow.shared.ui.ViewLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

/** Builds the main window: loads its view, applies the saved theme and shows it. */
@Component
public class MainWindow {

    private final ViewLoader views;
    private final ThemeManager theme;
    private final SettingsService settings;

    public MainWindow(ViewLoader views, ThemeManager theme, SettingsService settings) {
        this.views = views;
        this.theme = theme;
        this.settings = settings;
    }

    public void open(Stage stage) {
        ViewLoader.LoadedView<MainController> main = views.load("main");
        Scene scene = new Scene(main.root());
        theme.setDark(SettingsService.THEME_DARK.equals(settings.theme()));
        theme.apply(scene);
        main.controller().start(stage, scene);
    }
}
