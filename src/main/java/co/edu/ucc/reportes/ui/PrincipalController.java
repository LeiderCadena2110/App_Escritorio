package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.ServicioAjustes;
import java.util.EnumMap;
import java.util.Map;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Ventana principal: barra de menú, barra lateral fija con las 6 secciones y un área
 * central que cambia de vista (BorderPane). Recuerda la última sección y el tamaño.
 */
@Component
@Scope("prototype")
public class PrincipalController {

    @FXML private MenuBar menu;
    @FXML private VBox barraLateral;
    @FXML private StackPane contenido;

    private final Vistas vistas;
    private final ServicioAjustes ajustes;
    private final Navegador navegador;
    private final Dialogos dialogos;

    private final Map<Seccion, Vistas.Cargada<Object>> cargadas = new EnumMap<>(Seccion.class);
    private final Map<Seccion, ToggleButton> botones = new EnumMap<>(Seccion.class);
    private final ToggleGroup grupo = new ToggleGroup();
    private Stage escenario;
    private Seccion actual;

    public PrincipalController(Vistas vistas, ServicioAjustes ajustes, Navegador navegador, Dialogos dialogos) {
        this.vistas = vistas;
        this.ajustes = ajustes;
        this.navegador = navegador;
        this.dialogos = dialogos;
    }

    @FXML
    private void initialize() {
        navegador.registrar(this);
        construirBarraLateral();
        construirMenu();
        // En macOS el menú va arriba en la barra del sistema; en Windows y Linux, dentro de la ventana
        menu.setUseSystemMenuBar(true);
    }

    void iniciar(Stage escenario, Scene escena) {
        this.escenario = escenario;
        escenario.setTitle(Textos.t("app.titulo"));
        escenario.setMinWidth(900);
        escenario.setMinHeight(600);
        restaurarVentana();
        escenario.setScene(escena);
        escenario.setOnCloseRequest(e -> guardarVentana());

        Seccion inicial = Seccion.INICIO;
        try {
            inicial = Seccion.valueOf(ajustes.obtener(ServicioAjustes.ULTIMA_SECCION, Seccion.INICIO.name()));
        } catch (IllegalArgumentException ignorada) {
            // valor viejo: se usa Inicio
        }
        // El asistente siempre empieza limpio: si la última sección fue Generar, se abre en el paso 1
        ir(inicial);
        escenario.show();
    }

    public void ir(Seccion s) {
        Vistas.Cargada<Object> vista = cargadas.computeIfAbsent(s, x -> vistas.cargar(x.fxml()));
        contenido.getChildren().setAll(vista.raiz());
        botones.get(s).setSelected(true);
        actual = s;
        if (vista.controlador() instanceof VistaSeccion v) v.alMostrar();
        ajustes.guardar(ServicioAjustes.ULTIMA_SECCION, s.name());
    }

    @SuppressWarnings("unchecked")
    <T> T controlador(Seccion s) {
        return (T) cargadas.computeIfAbsent(s, x -> vistas.cargar(x.fxml())).controlador();
    }

    Window ventana() {
        return escenario;
    }

    Seccion actual() {
        return actual;
    }

    private void construirBarraLateral() {
        Label marca = new Label(Textos.t("app.tituloCorto"));
        marca.getStyleClass().add("marca");
        barraLateral.getChildren().add(marca);
        int n = 1;
        for (Seccion s : Seccion.values()) {
            ToggleButton b = new ToggleButton(n++ + "   " + Textos.t(s.claveTexto()));
            b.getStyleClass().add("boton-seccion");
            b.setToggleGroup(grupo);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setFocusTraversable(false);
            // Un clic sobre la sección activa no la deselecciona
            b.setOnAction(e -> ir(s));
            botones.put(s, b);
            barraLateral.getChildren().add(b);
        }
    }

    private void construirMenu() {
        MenuItem nuevo = item("menu.nuevoReporte", new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), navegador::nuevoReporte);
        MenuItem importar = item("menu.importarDatos", new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), navegador::importarDatos);
        MenuItem config = item("menu.configuracion", new KeyCodeCombination(KeyCode.COMMA, KeyCombination.SHORTCUT_DOWN), () -> ir(Seccion.CONFIGURACION));
        MenuItem salir = item("menu.salir", new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN), () -> {
            guardarVentana();
            escenario.close();
            javafx.application.Platform.exit();
        });
        Menu archivo = new Menu(Textos.t("menu.archivo"), null, nuevo, importar, new SeparatorMenuItem(), config, new SeparatorMenuItem(), salir);

        Menu ir = new Menu(Textos.t("menu.ir"));
        KeyCode[] numeros = {KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5, KeyCode.DIGIT6};
        for (Seccion s : Seccion.values()) {
            ir.getItems().add(item(s.claveTexto(), new KeyCodeCombination(numeros[s.ordinal()], KeyCombination.SHORTCUT_DOWN), () -> ir(s)));
        }

        MenuItem acerca = item("menu.acercaDe", null, () -> dialogos.info(escenario, Textos.t("menu.acercaDe"), Textos.t("acerca.texto")));
        Menu ayuda = new Menu(Textos.t("menu.ayuda"), null, acerca);
        menu.getMenus().setAll(archivo, ir, ayuda);
    }

    private static MenuItem item(String clave, KeyCombination atajo, Runnable accion) {
        MenuItem m = new MenuItem(Textos.t(clave));
        if (atajo != null) m.setAccelerator(atajo);
        m.setOnAction(e -> accion.run());
        return m;
    }

    private void restaurarVentana() {
        String[] v = ajustes.obtener(ServicioAjustes.VENTANA, "1180,760,false").split(",");
        try {
            double ancho = Double.parseDouble(v[0]);
            double alto = Double.parseDouble(v[1]);
            Rectangle2D pantalla = Screen.getPrimary().getVisualBounds();
            escenario.setWidth(Math.min(ancho, pantalla.getWidth()));
            escenario.setHeight(Math.min(alto, pantalla.getHeight()));
            escenario.centerOnScreen();
            escenario.setMaximized(Boolean.parseBoolean(v[2]));
        } catch (RuntimeException e) {
            escenario.setWidth(1180);
            escenario.setHeight(760);
        }
    }

    private void guardarVentana() {
        if (escenario == null) return;
        boolean max = escenario.isMaximized();
        ajustes.guardar(ServicioAjustes.VENTANA,
                (int) escenario.getWidth() + "," + (int) escenario.getHeight() + "," + max);
    }
}
