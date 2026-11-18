package co.edu.ucc.stockflow.ui;

import co.edu.ucc.stockflow.DesktopApp;
import co.edu.ucc.stockflow.service.DataSeeder;
import co.edu.ucc.stockflow.service.SettingsService;
import java.util.EnumMap;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Main window: menu bar, fixed sidebar with the sections and a center area that changes view.
 * Remembers the last section and the window size.
 */
@Component
@Scope("prototype")
public class MainController implements NavigationHost {

    @FXML
    private MenuBar menuBar;
    @FXML
    private VBox sidebar;
    @FXML
    private StackPane content;
    @FXML
    private HBox loadingBadge;

    private final ViewLoader views;
    private final SettingsService settings;
    private final Navigator navigator;
    private final Dialogs dialogs;
    private final TaskRunner tasks;
    private final DataSeeder seeder;

    private final Map<Section, ViewLoader.LoadedView<Object>> loaded = new EnumMap<>(Section.class);
    private final Map<Section, ToggleButton> buttons = new EnumMap<>(Section.class);
    private final ToggleGroup group = new ToggleGroup();
    private Stage stage;
    private Section current = Section.DASHBOARD;

    public MainController(ViewLoader views, SettingsService settings, Navigator navigator, Dialogs dialogs,
                          TaskRunner tasks, DataSeeder seeder) {
        this.views = views;
        this.settings = settings;
        this.navigator = navigator;
        this.dialogs = dialogs;
        this.tasks = tasks;
        this.seeder = seeder;
    }

    @FXML
    private void initialize() {
        navigator.register(this);
        buildSidebar();
        buildMenu();
        // On macOS the menu goes to the system bar; on Windows and Linux it stays in the window
        menuBar.setUseSystemMenuBar(true);
        setUpLoadingBadge();
    }

    /** "Cargando…" appears only if the work takes more than 300 ms, so quick queries do not blink. */
    private void setUpLoadingBadge() {
        PauseTransition delay = new PauseTransition(Duration.millis(300));
        delay.setOnFinished(e -> loadingBadge.setVisible(tasks.runningProperty().get() > 0));
        tasks.runningProperty().addListener((o, before, now) -> {
            if (now.intValue() > 0 && before.intValue() == 0) delay.playFromStart();
            if (now.intValue() == 0) {
                delay.stop();
                loadingBadge.setVisible(false);
            }
        });
    }

    public void start(Stage stage, Scene scene) {
        this.stage = stage;
        stage.setTitle(DesktopApp.NAME + " · " + settings.businessInfo().name());
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        restoreWindow();
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> saveWindow());

        Section first = Section.DASHBOARD;
        try {
            first = Section.valueOf(settings.get(SettingsService.LAST_SECTION, Section.DASHBOARD.name()));
        } catch (IllegalArgumentException ignored) {
            // old value: start at the dashboard
        }
        show(first);
        stage.show();
    }

    @Override
    public Object show(Section section) {
        current = section;
        ViewLoader.LoadedView<Object> view = loaded.computeIfAbsent(section, s -> views.load(s.fxml()));
        content.getChildren().setAll(view.root());
        buttons.get(section).setSelected(true);
        if (view.controller() instanceof SectionController c) c.onShow();
        settings.set(SettingsService.LAST_SECTION, section.name());
        return view.controller();
    }

    @Override
    public Window window() {
        return stage;
    }

    private void buildSidebar() {
        Label brand = new Label(DesktopApp.NAME);
        brand.getStyleClass().add("brand");
        Label subtitle = new Label("Inventario y facturación");
        subtitle.getStyleClass().add("brand-subtitle");
        sidebar.getChildren().addAll(brand, subtitle);
        for (Section s : Section.values()) {
            ToggleButton b = new ToggleButton(s.label());
            b.getStyleClass().add("nav-button");
            b.setToggleGroup(group);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setFocusTraversable(false);
            b.setOnAction(e -> show(s)); // clicking the active section keeps it selected
            buttons.put(s, b);
            sidebar.getChildren().add(b);
        }
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label footer = new Label("Ctrl + 1…9 para cambiar de sección");
        footer.getStyleClass().add("sidebar-footer");
        footer.setWrapText(true);
        sidebar.getChildren().addAll(spacer, footer);
    }

    private void buildMenu() {
        MenuItem newSale = item("Nueva venta", shortcut(KeyCode.N), () -> show(Section.POINT_OF_SALE));
        MenuItem settingsItem = item("Configuración", shortcut(KeyCode.COMMA), () -> show(Section.SETTINGS));
        MenuItem exit = item("Salir", shortcut(KeyCode.Q), () -> {
            saveWindow();
            stage.close();
            Platform.exit();
        });
        Menu file = new Menu("Archivo", null, newSale, new SeparatorMenuItem(), settingsItem,
                new SeparatorMenuItem(), exit);

        Menu go = new Menu("Ir a");
        KeyCode[] digits = {KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5,
                KeyCode.DIGIT6, KeyCode.DIGIT7, KeyCode.DIGIT8, KeyCode.DIGIT9};
        for (Section s : Section.values()) {
            go.getItems().add(item(s.label(), shortcut(digits[s.ordinal()]), () -> show(s)));
        }

        MenuItem about = item("Acerca de StockFlow", null, () -> dialogs.info(stage, "Acerca de StockFlow",
                "StockFlow 1.1\nInventario y facturación para pequeños negocios.\n\n"
                        + "Los datos se guardan en este equipo, en la carpeta .stockflow de su usuario.\n"
                        + "Universidad Cooperativa de Colombia, sede Pasto."));
        MenuItem sample = item("Cargar datos de ejemplo", null, this::loadSampleData);
        Menu help = new Menu("Ayuda", null, sample, new SeparatorMenuItem(), about);
        menuBar.getMenus().setAll(file, go, help);
    }

    /** Fills an empty business with the sample shoe-store catalog (to try the app or present it). */
    private void loadSampleData() {
        tasks.run("No se pudieron cargar los datos de ejemplo", seeder::canLoadSampleData, allowed -> {
            if (!allowed) {
                dialogs.info(stage, "Datos de ejemplo",
                        "Los datos de ejemplo solo se pueden cargar en un negocio sin productos, y una sola vez.");
                return;
            }
            if (!dialogs.confirm(stage, "Datos de ejemplo", "Se crearán 13 productos de calzado, 5 categorías, "
                    + "3 proveedores y 3 clientes de ejemplo. Puede editarlos o borrarlos después.", "Cargar")) {
                return;
            }
            tasks.run("No se pudieron cargar los datos de ejemplo", () -> {
                seeder.loadSampleData();
                return true;
            }, done -> show(current));
        });
    }

    private static KeyCombination shortcut(KeyCode code) {
        return new KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN);
    }

    private static MenuItem item(String text, KeyCombination accelerator, Runnable action) {
        MenuItem m = new MenuItem(text);
        if (accelerator != null) m.setAccelerator(accelerator);
        m.setOnAction(e -> action.run());
        return m;
    }

    private void restoreWindow() {
        String[] v = settings.get(SettingsService.WINDOW, "1240,780,false").split(",");
        try {
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            stage.setWidth(Math.min(Double.parseDouble(v[0]), screen.getWidth()));
            stage.setHeight(Math.min(Double.parseDouble(v[1]), screen.getHeight()));
            stage.centerOnScreen();
            stage.setMaximized(Boolean.parseBoolean(v[2]));
        } catch (RuntimeException e) {
            stage.setWidth(1240);
            stage.setHeight(780);
        }
    }

    private void saveWindow() {
        if (stage == null) return;
        settings.set(SettingsService.WINDOW,
                (int) stage.getWidth() + "," + (int) stage.getHeight() + "," + stage.isMaximized());
    }
}
