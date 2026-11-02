package co.edu.ucc.stockflow.ui;

import co.edu.ucc.stockflow.service.SettingsService;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import org.springframework.stereotype.Component;

/** Light or dark theme (Settings › Appearance). Applied instantly. */
@Component
public class ThemeManager {

    private static final String MAIN = "/styles/main.css";
    private static final String DARK = "/styles/dark.css";

    private final SettingsService settings;
    private Scene mainScene;

    public ThemeManager(SettingsService settings) {
        this.settings = settings;
    }

    public void apply(Scene scene) {
        this.mainScene = scene;
        scene.getStylesheets().setAll(stylesheets());
    }

    /** Dialogs use the same theme as the window. */
    public void apply(DialogPane pane) {
        pane.getStylesheets().setAll(stylesheets());
    }

    public void change(String theme) {
        settings.set(SettingsService.THEME, theme);
        if (mainScene != null) mainScene.getStylesheets().setAll(stylesheets());
    }

    private String[] stylesheets() {
        String main = ThemeManager.class.getResource(MAIN).toExternalForm();
        if (SettingsService.THEME_DARK.equals(settings.theme())) {
            return new String[]{main, ThemeManager.class.getResource(DARK).toExternalForm()};
        }
        return new String[]{main};
    }
}
