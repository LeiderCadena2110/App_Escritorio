package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.FormatoValores;
import co.edu.ucc.reportes.service.ServicioAjustes;
import java.io.File;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** 6. Configuración: carpeta de salida, formatos de fecha y número, idioma y tema. */
@Component
@Scope("prototype")
public class AjustesController implements VistaSeccion {

    private static final String[] PATRONES_FECHA = {"dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "d 'de' MMMM 'de' yyyy", "MMMM d, yyyy"};
    private static final Map<String, String> NUMEROS = new LinkedHashMap<>();
    private static final Map<String, String> IDIOMAS = new LinkedHashMap<>();

    static {
        NUMEROS.put("es-CO", "1.234.567,89");
        NUMEROS.put("en-US", "1,234,567.89");
        NUMEROS.put("fr-FR", "1 234 567,89");
        IDIOMAS.put("es", "Español");
        IDIOMAS.put("en", "English");
    }

    @FXML private TextField carpeta;
    @FXML private ComboBox<String> formatoFecha;
    @FXML private ComboBox<String> formatoNumero;
    @FXML private ComboBox<String> idioma;
    @FXML private Label avisoIdioma;
    @FXML private ToggleGroup grupoTema;
    @FXML private RadioButton temaClaro;
    @FXML private RadioButton temaOscuro;
    @FXML private Label ejemplo;

    private final ServicioAjustes ajustes;
    private final Tema tema;
    private final Navegador navegador;
    private boolean cargando;

    public AjustesController(ServicioAjustes ajustes, Tema tema, Navegador navegador) {
        this.ajustes = ajustes;
        this.tema = tema;
        this.navegador = navegador;
    }

    @FXML
    private void initialize() {
        avisoIdioma.managedProperty().bind(avisoIdioma.visibleProperty());
        formatoFecha.getItems().setAll(PATRONES_FECHA);
        formatoFecha.setConverter(convertidor(p -> p + "   →   " + ejemploFecha(p)));
        formatoNumero.getItems().setAll(NUMEROS.keySet());
        formatoNumero.setConverter(convertidor(NUMEROS::get));
        idioma.getItems().setAll(IDIOMAS.keySet());
        idioma.setConverter(convertidor(IDIOMAS::get));

        formatoFecha.valueProperty().addListener((o, a, n) -> guardar(ServicioAjustes.FORMATO_FECHA, n));
        formatoNumero.valueProperty().addListener((o, a, n) -> guardar(ServicioAjustes.FORMATO_NUMERO, n));
        idioma.valueProperty().addListener((o, a, n) -> {
            guardar(ServicioAjustes.IDIOMA, n);
            avisoIdioma.setVisible(n != null && !n.equals(Textos.idioma().getLanguage()));
        });
        grupoTema.selectedToggleProperty().addListener((o, a, n) -> {
            if (!cargando && n != null) tema.cambiar(n == temaOscuro ? ServicioAjustes.TEMA_OSCURO : ServicioAjustes.TEMA_CLARO);
        });
    }

    @Override
    public void alMostrar() {
        cargando = true;
        carpeta.setText(ajustes.carpetaSalida().toString());
        formatoFecha.setValue(ajustes.patronFecha());
        formatoNumero.setValue(ajustes.localeNumeros().toLanguageTag());
        idioma.setValue(ajustes.idioma().getLanguage());
        avisoIdioma.setVisible(false);
        (ServicioAjustes.TEMA_OSCURO.equals(ajustes.tema()) ? temaOscuro : temaClaro).setSelected(true);
        cargando = false;
        actualizarEjemplo();
    }

    @FXML
    private void elegirCarpeta() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle(Textos.t("generar.elegirCarpeta"));
        File actual = new File(carpeta.getText());
        if (actual.isDirectory()) dc.setInitialDirectory(actual);
        File f = dc.showDialog(navegador.ventana());
        if (f != null) {
            carpeta.setText(f.getAbsolutePath());
            ajustes.guardar(ServicioAjustes.CARPETA_SALIDA, f.getAbsolutePath());
        }
    }

    @FXML
    private void carpetaPorDefecto() {
        ajustes.guardar(ServicioAjustes.CARPETA_SALIDA, null);
        carpeta.setText(ajustes.carpetaSalida().toString());
    }

    private void guardar(String clave, String valor) {
        if (cargando || valor == null) return;
        ajustes.guardar(clave, valor);
        actualizarEjemplo();
    }

    private void actualizarEjemplo() {
        FormatoValores f = ajustes.formatoValores();
        ejemplo.setText(Textos.t("ajustes.ejemplo", f.fecha(LocalDateTime.of(2026, 3, 15, 0, 0)), f.numeroConMiles(1234567.89)));
    }

    private String ejemploFecha(String patron) {
        try {
            return new FormatoValores(patron, Locale.ROOT, ajustes.idioma()).fecha(LocalDateTime.of(2026, 3, 15, 0, 0));
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static StringConverter<String> convertidor(java.util.function.Function<String, String> texto) {
        return new StringConverter<>() {
            @Override
            public String toString(String v) {
                return v == null ? "" : texto.apply(v);
            }

            @Override
            public String fromString(String s) {
                return s;
            }
        };
    }
}
