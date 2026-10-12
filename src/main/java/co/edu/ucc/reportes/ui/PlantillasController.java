package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.domain.Plantilla;
import co.edu.ucc.reportes.domain.TipoPlantilla;
import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.ServicioPlantillas;
import java.io.File;
import java.nio.file.Path;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** 3. Plantillas: lista + detalle con los marcadores detectados. */
@Component
@Scope("prototype")
public class PlantillasController implements VistaSeccion {

    @FXML private ListView<Plantilla> lista;
    @FXML private VBox detalle;
    @FXML private Label nombre;
    @FXML private Label tipo;
    @FXML private Label descripcion;
    @FXML private ListView<String> marcadores;
    @FXML private Label infoTabla;
    @FXML private Button abrirWord;
    @FXML private Button volverALeer;
    @FXML private Button eliminar;
    @FXML private Label sinSeleccion;

    private final ServicioPlantillas servicio;
    private final Navegador navegador;
    private final Dialogos dialogos;
    private final Escritorio escritorio;

    public PlantillasController(ServicioPlantillas servicio, Navegador navegador, Dialogos dialogos, Escritorio escritorio) {
        this.servicio = servicio;
        this.navegador = navegador;
        this.dialogos = dialogos;
        this.escritorio = escritorio;
    }

    @FXML
    private void initialize() {
        lista.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Plantilla p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label n = new Label(p.getNombre());
                n.getStyleClass().add("lista-titulo");
                Label etiqueta = new Label(p.getTipo() == TipoPlantilla.DOCX ? "WORD" : "PDF");
                etiqueta.getStyleClass().addAll("etiqueta", p.getTipo() == TipoPlantilla.DOCX ? "etiqueta-word" : "etiqueta-pdf");
                Region r = new Region();
                HBox.setHgrow(r, Priority.ALWAYS);
                Label sub = new Label(p.isIntegrada() ? Textos.t("plantillas.integrada")
                        : Textos.t("plantillas.marcadores", p.camposPorMapear().size()));
                sub.getStyleClass().add("lista-sub");
                setText(null);
                setGraphic(new VBox(2, new HBox(8, n, r, etiqueta), sub));
            }
        });
        lista.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> mostrar(n));
        marcadores.setPlaceholder(new Label(Textos.t("plantillas.sinMarcadores")));
        mostrar(null);
    }

    @Override
    public void alMostrar() {
        Plantilla sel = lista.getSelectionModel().getSelectedItem();
        lista.getItems().setAll(servicio.listar());
        if (sel != null) seleccionar(sel.getId());
    }

    @FXML
    private void registrar() {
        FileChooser fc = new FileChooser();
        fc.setTitle(Textos.t("plantillas.elegirDocx"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word (.docx)", "*.docx"));
        File f = fc.showOpenDialog(navegador.ventana());
        if (f == null) return;
        String sugerido = f.getName().replaceFirst("(?i)\\.docx$", "");
        dialogos.pedirTexto(navegador.ventana(), Textos.t("plantillas.registrar"), Textos.t("plantillas.nombre"), sugerido)
                .ifPresent(n -> {
                    Task<Plantilla> tarea = new Task<>() {
                        @Override
                        protected Plantilla call() throws Exception {
                            return servicio.registrarDocx(f.toPath(), n);
                        }
                    };
                    tarea.setOnSucceeded(e -> {
                        alMostrar();
                        seleccionar(tarea.getValue().getId());
                        Plantilla p = tarea.getValue();
                        if (p.camposPorMapear().isEmpty()) {
                            dialogos.info(navegador.ventana(), Textos.t("plantillas.sinMarcadores.titulo"), Textos.t("plantillas.sinMarcadores.ayuda"));
                        }
                    });
                    tarea.setOnFailed(e -> dialogos.error(navegador.ventana(), Textos.t("plantillas.errorRegistrar"), tarea.getException()));
                    SesionTrabajo.ejecutar(tarea, "registrar-plantilla");
                });
    }

    @FXML
    private void abrirWord() {
        Plantilla p = lista.getSelectionModel().getSelectedItem();
        if (p != null && p.getTipo() == TipoPlantilla.DOCX) {
            escritorio.abrir(Path.of(p.getRuta()));
            dialogos.info(navegador.ventana(), Textos.t("plantillas.abrirWord"), Textos.t("plantillas.abrirWord.ayuda"));
        }
    }

    @FXML
    private void volverALeer() {
        Plantilla p = lista.getSelectionModel().getSelectedItem();
        if (p == null) return;
        try {
            Plantilla actualizada = servicio.volverALeer(p);
            alMostrar();
            seleccionar(actualizada.getId());
        } catch (Exception e) {
            dialogos.error(navegador.ventana(), Textos.t("plantillas.errorLeer"), e);
        }
    }

    @FXML
    private void eliminar() {
        Plantilla p = lista.getSelectionModel().getSelectedItem();
        if (p == null || p.isIntegrada()) return;
        if (dialogos.confirmar(navegador.ventana(), Textos.t("plantillas.eliminar.titulo"),
                Textos.t("plantillas.eliminar.mensaje", p.getNombre()), Textos.t("comun.eliminar"))) {
            try {
                servicio.eliminar(p);
                alMostrar();
            } catch (Exception e) {
                dialogos.error(navegador.ventana(), Textos.t("plantillas.eliminar.titulo"), e);
            }
        }
    }

    @FXML
    private void usar() {
        Plantilla p = lista.getSelectionModel().getSelectedItem();
        if (p != null) navegador.usarPlantilla(p);
    }

    private void seleccionar(Long id) {
        for (Plantilla p : lista.getItems()) {
            if (p.getId().equals(id)) {
                lista.getSelectionModel().select(p);
                lista.scrollTo(p);
                return;
            }
        }
    }

    private void mostrar(Plantilla p) {
        detalle.setVisible(p != null);
        sinSeleccion.setVisible(p == null);
        if (p == null) return;
        nombre.setText(p.getNombre());
        tipo.setText(Formatos.tipo(p.getTipo()));
        descripcion.setText(p.getDescripcion() == null ? "" : p.getDescripcion());
        marcadores.getItems().clear();
        for (String m : p.listaMarcadores()) {
            marcadores.getItems().add(p.getTipo() == TipoPlantilla.DOCX ? "{{" + m + "}}" : m);
        }
        for (String c : p.listaColumnasTabla()) marcadores.getItems().add("[" + c + "]   " + Textos.t("plantillas.columnaTabla"));
        if (p.getTipo() == TipoPlantilla.HTML && p.camposPorMapear().isEmpty()) {
            infoTabla.setText(Textos.t("plantillas.todasLasColumnas"));
        } else if (p.isTieneTabla()) {
            infoTabla.setText(Textos.t("plantillas.conTabla"));
        } else if (p.getTipo() == TipoPlantilla.DOCX) {
            infoTabla.setText(Textos.t("plantillas.sinTabla"));
        } else {
            infoTabla.setText(Textos.t("plantillas.htmlModos"));
        }
        boolean word = p.getTipo() == TipoPlantilla.DOCX;
        abrirWord.setDisable(!word);
        volverALeer.setDisable(!word);
        eliminar.setDisable(p.isIntegrada());
    }
}
