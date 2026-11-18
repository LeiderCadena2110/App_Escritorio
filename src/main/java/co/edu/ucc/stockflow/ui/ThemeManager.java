package co.edu.ucc.stockflow.ui;

import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import org.springframework.stereotype.Component;

/**
 * Light or dark theme, applied instantly to the main window and every dialog.
 * It only knows how to paint; which theme the user chose is saved by the settings module.
 */
@Component
public class ThemeManager {

    private static final String MAIN = "/styles/main.css";
    private static final String DARK = "/styles/dark.css";

    private Scene mainScene;
    private boolean dark;

    public void apply(Scene scene) {
        this.mainScene = scene;
        scene.getStylesheets().setAll(stylesheets());
    }

    /** Dialogs use the same theme as the window. */
    public void apply(DialogPane pane) {
        pane.getStylesheets().setAll(stylesheets());
    }

    public void setDark(boolean dark) {
        this.dark = dark;
        if (mainScene != null) mainScene.getStylesheets().setAll(stylesheets());
    }

    public boolean isDark() {
        return dark;
    }

    private String[] stylesheets() {
        String main = ThemeManager.class.getResource(MAIN).toExternalForm();
        if (dark) return new String[]{main, ThemeManager.class.getResource(DARK).toExternalForm()};
        return new String[]{main};
    }
}
