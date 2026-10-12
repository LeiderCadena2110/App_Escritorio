package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.domain.Ejecucion;
import co.edu.ucc.reportes.domain.PerfilMapeo;
import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.ServicioHistorial;
import co.edu.ucc.reportes.service.ServicioPerfiles;
import java.nio.file.Path;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** 1. Inicio: reportes recientes y perfiles guardados. */
@Component
@Scope("prototype")
public class InicioController implements VistaSeccion {

    @FXML private ListView<Ejecucion> recientes;
    @FXML private ListView<PerfilMapeo> perfiles;
    @FXML private Button usarPerfil;
    @FXML private Button eliminarPerfil;
    @FXML private Button abrirCarpeta;

    private final ServicioHistorial historial;
    private final ServicioPerfiles servicioPerfiles;
    private final Navegador navegador;
    private final Escritorio escritorio;
    private final Dialogos dialogos;

    public InicioController(ServicioHistorial historial, ServicioPerfiles servicioPerfiles, Navegador navegador,
                            Escritorio escritorio, Dialogos dialogos) {
        this.historial = historial;
        this.servicioPerfiles = servicioPerfiles;
        this.navegador = navegador;
        this.escritorio = escritorio;
        this.dialogos = dialogos;
    }

    @FXML
    private void initialize() {
        recientes.setPlaceholder(new Label(Textos.t("inicio.sinRecientes")));
        perfiles.setPlaceholder(new Label(Textos.t("inicio.sinPerfiles")));
        recientes.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Ejecucion e, boolean vacio) {
                super.updateItem(e, vacio);
                if (vacio || e == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label titulo = new Label(e.getPlantillaNombre() + "  ·  " + Formatos.estado(e.getEstado()));
                titulo.getStyleClass().add("lista-titulo");
                Label sub = new Label(Formatos.fechaHora(e.getFecha()) + "  ·  "
                        + Textos.t("inicio.informes", e.getInformesGenerados(), e.getFormato().toUpperCase())
                        + "  ·  " + Path.of(e.getArchivoDatos()).getFileName());
                sub.getStyleClass().add("lista-sub");
                setText(null);
                setGraphic(new VBox(2, titulo, sub));
            }
        });
        perfiles.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(PerfilMapeo p, boolean vacio) {
                super.updateItem(p, vacio);
                if (vacio || p == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label titulo = new Label(p.getNombre());
                titulo.getStyleClass().add("lista-titulo");
                Label sub = new Label(p.getPlantilla().getNombre() + "  ·  " + Formatos.modo(p.getModo())
                        + (p.getUltimoUso() == null ? "" : "  ·  " + Textos.t("inicio.usado", Formatos.fechaHora(p.getUltimoUso()))));
                sub.getStyleClass().add("lista-sub");
                setText(null);
                setGraphic(new VBox(2, titulo, sub));
            }
        });
        perfiles.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) usarPerfil();
        });
        recientes.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) abrirCarpeta();
        });
        usarPerfil.disableProperty().bind(perfiles.getSelectionModel().selectedItemProperty().isNull());
        eliminarPerfil.disableProperty().bind(perfiles.getSelectionModel().selectedItemProperty().isNull());
        abrirCarpeta.disableProperty().bind(recientes.getSelectionModel().selectedItemProperty().isNull());
    }

    @Override
    public void alMostrar() {
        recientes.getItems().setAll(historial.recientes());
        perfiles.getItems().setAll(servicioPerfiles.listar());
    }

    @FXML
    private void nuevoReporte() {
        navegador.nuevoReporte();
    }

    @FXML
    private void importarDatos() {
        navegador.importarDatos();
    }

    @FXML
    private void verHistorial() {
        navegador.ir(Seccion.HISTORIAL);
    }

    @FXML
    private void usarPerfil() {
        PerfilMapeo p = perfiles.getSelectionModel().getSelectedItem();
        if (p != null) navegador.usarPerfil(p);
    }

    @FXML
    private void eliminarPerfil() {
        PerfilMapeo p = perfiles.getSelectionModel().getSelectedItem();
        if (p == null) return;
        if (dialogos.confirmar(navegador.ventana(), Textos.t("inicio.eliminarPerfil.titulo"),
                Textos.t("inicio.eliminarPerfil.mensaje", p.getNombre()), Textos.t("comun.eliminar"))) {
            servicioPerfiles.eliminar(p);
            alMostrar();
        }
    }

    @FXML
    private void abrirCarpeta() {
        Ejecucion e = recientes.getSelectionModel().getSelectedItem();
        if (e != null) escritorio.abrir(Path.of(e.getCarpetaSalida()));
    }
}
