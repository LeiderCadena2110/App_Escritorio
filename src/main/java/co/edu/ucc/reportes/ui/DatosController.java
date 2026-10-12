package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.LectorDatos;
import co.edu.ucc.reportes.service.Problema;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** 2. Datos: importar Excel o CSV, vista previa y validación. */
@Component
@Scope("prototype")
public class DatosController implements VistaSeccion {

    @FXML private VBox raiz;
    @FXML private Label archivo;
    @FXML private Label detalle;
    @FXML private HBox cajaHoja;
    @FXML private ComboBox<String> hoja;
    @FXML private ProgressIndicator cargando;
    @FXML private TableView<List<String>> vista;
    @FXML private ListView<Problema> problemas;
    @FXML private Label resumen;
    @FXML private Label tituloProblemas;
    @FXML private Button usarDatos;

    private final SesionTrabajo sesion;
    private final Navegador navegador;
    private final Dialogos dialogos;
    private boolean llenandoHojas;

    public DatosController(SesionTrabajo sesion, Navegador navegador, Dialogos dialogos) {
        this.sesion = sesion;
        this.navegador = navegador;
        this.dialogos = dialogos;
    }

    @FXML
    private void initialize() {
        vista.setPlaceholder(new Label(Textos.t("datos.vacio")));
        problemas.setPlaceholder(new Label(Textos.t("datos.sinProblemas")));
        TablaVistaPrevia.estilizarProblemas(problemas);
        cargando.setVisible(false);
        hoja.valueProperty().addListener((o, a, n) -> {
            SesionTrabajo.DatosCargados d = sesion.getDatos();
            if (!llenandoHojas && n != null && d != null && !n.equals(d.tabla().hoja())) leer(d.tabla().archivo(), n);
        });
        sesion.datosProperty().addListener((o, a, n) -> mostrar(n));
        // Arrastrar un archivo sobre la sección también lo importa
        raiz.setOnDragOver(this::arrastrando);
        raiz.setOnDragDropped(this::soltar);
        mostrar(sesion.getDatos());
    }

    @FXML
    public void importar() {
        FileChooser fc = selectorDatos();
        File f = fc.showOpenDialog(navegador.ventana());
        if (f != null) leer(f.toPath(), null);
    }

    @FXML
    private void usarDatos() {
        navegador.ir(Seccion.GENERAR);
    }

    static FileChooser selectorDatos() {
        FileChooser fc = new FileChooser();
        fc.setTitle(Textos.t("datos.elegirArchivo"));
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(Textos.t("datos.filtroTodos"), "*.xlsx", "*.xls", "*.xlsm", "*.csv", "*.txt"),
                new FileChooser.ExtensionFilter("Excel", "*.xlsx", "*.xls", "*.xlsm"),
                new FileChooser.ExtensionFilter("CSV", "*.csv", "*.txt"));
        return fc;
    }

    private void leer(Path ruta, String nombreHoja) {
        Task<SesionTrabajo.DatosCargados> tarea = sesion.tareaLeer(ruta, nombreHoja);
        cargando.setVisible(true);
        tarea.setOnSucceeded(e -> {
            cargando.setVisible(false);
            sesion.datosProperty().set(tarea.getValue());
        });
        tarea.setOnFailed(e -> {
            cargando.setVisible(false);
            dialogos.error(navegador.ventana(), Textos.t("datos.errorLeer", ruta.getFileName()), tarea.getException());
        });
        SesionTrabajo.ejecutar(tarea, "leer-datos");
    }

    private void mostrar(SesionTrabajo.DatosCargados d) {
        boolean hay = d != null;
        usarDatos.setDisable(!hay || d.hayErrores());
        cajaHoja.setVisible(hay && !d.hojas().isEmpty());
        cajaHoja.setManaged(cajaHoja.isVisible());
        if (!hay) {
            archivo.setText(Textos.t("datos.ningunArchivo"));
            detalle.setText(Textos.t("datos.ayuda"));
            resumen.setText("");
            problemas.getItems().clear();
            TablaVistaPrevia.llenar(vista, null);
            return;
        }
        archivo.setText(d.tabla().nombreArchivo());
        detalle.setText(d.tabla().detalle() + "  ·  " + d.tabla().archivo().getParent());
        llenandoHojas = true;
        hoja.getItems().setAll(d.hojas());
        hoja.setValue(d.tabla().hoja());
        llenandoHojas = false;
        TablaVistaPrevia.llenar(vista, d.tabla());
        problemas.getItems().setAll(d.problemas());
        String texto = TablaVistaPrevia.resumen(d.tabla(), d.problemas());
        if (d.tabla().totalFilas() > TablaVistaPrevia.MAX_FILAS) texto += "  ·  " + Textos.t("datos.vistaParcial", TablaVistaPrevia.MAX_FILAS);
        resumen.setText(texto);
        tituloProblemas.setText(Textos.t("datos.problemasTitulo", d.problemas().size()));
    }

    private void arrastrando(DragEvent e) {
        if (e.getDragboard().hasFiles() && e.getDragboard().getFiles().size() == 1) {
            Path p = e.getDragboard().getFiles().get(0).toPath();
            if (LectorDatos.esExcel(p) || LectorDatos.esCsv(p)) e.acceptTransferModes(TransferMode.COPY);
        }
        e.consume();
    }

    private void soltar(DragEvent e) {
        boolean ok = false;
        if (e.getDragboard().hasFiles()) {
            leer(e.getDragboard().getFiles().get(0).toPath(), null);
            ok = true;
        }
        e.setDropCompleted(ok);
        e.consume();
    }
}
