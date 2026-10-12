package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.domain.Ejecucion;
import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.ServicioHistorial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** 5. Historial: lista de ejecuciones + detalle y abrir la carpeta de salida. */
@Component
@Scope("prototype")
public class HistorialController implements VistaSeccion {

    @FXML private TableView<Ejecucion> tabla;
    @FXML private VBox detalle;
    @FXML private Label detalleTitulo;
    @FXML private Label detalleDatos;
    @FXML private Label detalleCarpeta;
    @FXML private Label detalleResumen;
    @FXML private TextArea detalleMensajes;
    @FXML private Button abrirCarpeta;
    @FXML private Button borrar;

    private final ServicioHistorial historial;
    private final Escritorio escritorio;
    private final Dialogos dialogos;
    private final Navegador navegador;

    public HistorialController(ServicioHistorial historial, Escritorio escritorio, Dialogos dialogos, Navegador navegador) {
        this.historial = historial;
        this.escritorio = escritorio;
        this.dialogos = dialogos;
        this.navegador = navegador;
    }

    @FXML
    private void initialize() {
        tabla.setPlaceholder(new Label(Textos.t("historial.vacio")));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        columna("historial.col.fecha", e -> Formatos.fechaHora(e.getFecha()), 130);
        columna("historial.col.plantilla", Ejecucion::getPlantillaNombre, 170);
        columna("historial.col.datos", e -> Path.of(e.getArchivoDatos()).getFileName().toString(), 130);
        columna("historial.col.modo", e -> Formatos.modo(e.getModo()), 120);
        columna("historial.col.informes", e -> e.getInformesGenerados() + " " + e.getFormato().toUpperCase(), 90);
        columna("historial.col.estado", e -> Formatos.estado(e.getEstado()), 120);
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> mostrar(n));
        tabla.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) abrirCarpeta();
        });
        mostrar(null);
    }

    @Override
    public void alMostrar() {
        tabla.getItems().setAll(historial.listar());
        borrar.setDisable(tabla.getItems().isEmpty());
        if (!tabla.getItems().isEmpty()) tabla.getSelectionModel().selectFirst();
    }

    @FXML
    private void abrirCarpeta() {
        Ejecucion e = tabla.getSelectionModel().getSelectedItem();
        if (e == null) return;
        Path carpeta = Path.of(e.getCarpetaSalida());
        if (Files.isDirectory(carpeta)) {
            escritorio.abrir(carpeta);
        } else {
            dialogos.error(navegador.ventana(), Textos.t("historial.carpetaNoExiste.titulo"), Textos.t("historial.carpetaNoExiste", carpeta));
        }
    }

    @FXML
    private void borrarHistorial() {
        if (dialogos.confirmar(navegador.ventana(), Textos.t("historial.borrar.titulo"), Textos.t("historial.borrar.mensaje"),
                Textos.t("comun.borrar"))) {
            historial.borrarTodo();
            alMostrar();
        }
    }

    private void columna(String clave, Function<Ejecucion, String> valor, double ancho) {
        TableColumn<Ejecucion, String> c = new TableColumn<>(Textos.t(clave));
        c.setCellValueFactory(d -> new ReadOnlyStringWrapper(valor.apply(d.getValue())));
        c.setPrefWidth(ancho);
        tabla.getColumns().add(c);
    }

    private void mostrar(Ejecucion e) {
        detalle.setDisable(e == null);
        abrirCarpeta.setDisable(e == null);
        if (e == null) {
            detalleTitulo.setText(Textos.t("historial.seleccione"));
            detalleDatos.setText("");
            detalleCarpeta.setText("");
            detalleResumen.setText("");
            detalleMensajes.setText("");
            return;
        }
        detalleTitulo.setText(e.getPlantillaNombre() + "  ·  " + Formatos.estado(e.getEstado()));
        detalleDatos.setText(Textos.t("historial.detalle.datos", e.getArchivoDatos()));
        detalleCarpeta.setText(Textos.t("historial.detalle.carpeta", e.getCarpetaSalida()));
        detalleResumen.setText(Textos.t("historial.detalle.resumen", e.getInformesGenerados(), e.getFilasProcesadas(),
                Formatos.modo(e.getModo()), Formatos.duracion(e.getDuracionMs()), Formatos.fechaHora(e.getFecha())));
        detalleMensajes.setText(e.getMensajes() == null || e.getMensajes().isBlank() ? Textos.t("historial.sinErrores") : e.getMensajes());
    }
}
