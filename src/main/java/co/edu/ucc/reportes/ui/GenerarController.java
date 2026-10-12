package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.domain.EstadoEjecucion;
import co.edu.ucc.reportes.domain.ModoSalida;
import co.edu.ucc.reportes.domain.PerfilMapeo;
import co.edu.ucc.reportes.domain.Plantilla;
import co.edu.ucc.reportes.domain.TipoPlantilla;
import co.edu.ucc.reportes.i18n.Textos;
import co.edu.ucc.reportes.service.GeneradorInforme;
import co.edu.ucc.reportes.service.ServicioAjustes;
import co.edu.ucc.reportes.service.ServicioHistorial;
import co.edu.ucc.reportes.service.ServicioPerfiles;
import co.edu.ucc.reportes.service.ServicioPlantillas;
import co.edu.ucc.reportes.service.SugeridorMapeo;
import co.edu.ucc.reportes.service.TablaDatos;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * 4. Generar reporte: asistente de 4 pasos (Datos › Plantilla › Mapeo › Salida),
 * luego "Generando" con progreso y Cancelar, y el Resultado.
 * Siguiente solo se activa cuando el paso es válido; Atrás no pierde lo elegido.
 */
@Component
@Scope("prototype")
public class GenerarController implements VistaSeccion {

    private static final int PASOS = 4;

    // Estructura
    @FXML private VBox raiz;
    @FXML private HBox indicador;
    @FXML private VBox paso1;
    @FXML private VBox paso2;
    @FXML private VBox paso3;
    @FXML private VBox paso4;
    @FXML private VBox generando;
    @FXML private VBox resultado;
    @FXML private HBox botonera;
    @FXML private Button atras;
    @FXML private Button siguiente;
    @FXML private Button cancelar;

    // Paso 1
    @FXML private Label datosArchivo;
    @FXML private Label datosDetalle;
    @FXML private Label datosResumen;
    @FXML private HBox cajaHoja;
    @FXML private ComboBox<String> hoja;
    @FXML private Label datosErrores;

    // Paso 2
    @FXML private ListView<Plantilla> plantillas;
    @FXML private Label plantillaDescripcion;

    // Paso 3
    @FXML private GridPane gridMapeo;
    @FXML private Label mapeoEstado;
    @FXML private Label mapeoVacio;

    // Paso 4
    @FXML private RadioButton modoPorFila;
    @FXML private RadioButton modoTabla;
    @FXML private ToggleGroup grupoModo;
    @FXML private Label modoAviso;
    @FXML private Label formatoSalida;
    @FXML private TextField carpeta;
    @FXML private ComboBox<String> columnaNombre;
    @FXML private HBox cajaNombre;
    @FXML private CheckBox guardarPerfil;
    @FXML private TextField nombrePerfil;

    // Generando y resultado
    @FXML private ProgressBar barra;
    @FXML private Label progresoTexto;
    @FXML private Button cancelarGeneracion;
    @FXML private Label resultadoTitulo;
    @FXML private Label resultadoTexto;
    @FXML private ListView<String> resultadoErrores;

    private final SesionTrabajo sesion;
    private final ServicioPlantillas servicioPlantillas;
    private final ServicioPerfiles servicioPerfiles;
    private final ServicioHistorial historial;
    private final ServicioAjustes ajustes;
    private final GeneradorInforme generador;
    private final Navegador navegador;
    private final Dialogos dialogos;
    private final Escritorio escritorio;

    private int paso = 1;
    private boolean ocupado;
    private final AtomicBoolean pedirCancelar = new AtomicBoolean();
    private final Map<String, ComboBox<String>> combos = new LinkedHashMap<>();
    private final Map<String, Label> etiquetasMapeo = new LinkedHashMap<>();
    /** Mapeo elegido; se conserva al ir Atrás y al cambiar de archivo. */
    private final Map<String, String> mapeo = new LinkedHashMap<>();
    private PerfilMapeo perfilOrigen;
    private Plantilla plantillaInicial;
    private GeneradorInforme.Resultado ultimo;
    private boolean atajosRegistrados;

    public GenerarController(SesionTrabajo sesion, ServicioPlantillas servicioPlantillas, ServicioPerfiles servicioPerfiles,
                             ServicioHistorial historial, ServicioAjustes ajustes, GeneradorInforme generador,
                             Navegador navegador, Dialogos dialogos, Escritorio escritorio) {
        this.sesion = sesion;
        this.servicioPlantillas = servicioPlantillas;
        this.servicioPerfiles = servicioPerfiles;
        this.historial = historial;
        this.ajustes = ajustes;
        this.generador = generador;
        this.navegador = navegador;
        this.dialogos = dialogos;
        this.escritorio = escritorio;
    }

    @FXML
    private void initialize() {
        construirIndicador();
        plantillas.setCellFactory(l -> new ListCell<>() {
            @Override
            protected void updateItem(Plantilla p, boolean vacio) {
                super.updateItem(p, vacio);
                setText(vacio || p == null ? null : p.getNombre() + "   ·   " + Formatos.tipo(p.getTipo()));
            }
        });
        plantillas.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> {
            plantillaDescripcion.setText(n == null ? "" : (n.getDescripcion() == null ? "" : n.getDescripcion()));
            actualizarBotones();
        });
        plantillas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && paso == 2 && valido(2)) siguiente();
        });
        hoja.valueProperty().addListener((o, a, n) -> {
            SesionTrabajo.DatosCargados d = sesion.getDatos();
            if (n != null && d != null && !n.equals(d.tabla().hoja())) leer(d.tabla().archivo(), n);
        });
        sesion.datosProperty().addListener((o, a, n) -> {
            if (!ocupado) {
                mostrarDatos();
                actualizarBotones();
            }
        });
        grupoModo.selectedToggleProperty().addListener((o, a, n) -> actualizarModo());
        guardarPerfil.selectedProperty().addListener((o, a, n) -> {
            nombrePerfil.setDisable(!n);
            actualizarBotones();
        });
        nombrePerfil.textProperty().addListener((o, a, n) -> actualizarBotones());
        columnaNombre.setConverter(convertidorColumna());
        resultadoErrores.setPlaceholder(new Label(""));

        // Atajos de "Paso anterior": Cmd+[ en macOS, Alt+← en Windows y Linux
        raiz.sceneProperty().addListener((o, a, escena) -> registrarAtajos(escena));
        reiniciar(null, null);
    }

    @Override
    public void alMostrar() {
        if (ocupado) return;
        if (paso == 1) mostrarDatos();
        actualizarBotones();
    }

    /** Empieza el asistente desde el paso 1, opcionalmente con un perfil o una plantilla. */
    public void reiniciar(PerfilMapeo perfil, Plantilla plantilla) {
        if (ocupado) return;
        perfilOrigen = perfil;
        plantillaInicial = perfil != null ? perfil.getPlantilla() : plantilla;
        mapeo.clear();
        if (perfil != null) mapeo.putAll(perfil.getMapeo());
        guardarPerfil.setSelected(false);
        nombrePerfil.setText(perfil != null ? perfil.getNombre() : "");
        nombrePerfil.setDisable(true);
        modoPorFila.setSelected(perfil == null || perfil.getModo() == ModoSalida.POR_FILA);
        modoTabla.setSelected(perfil != null && perfil.getModo() == ModoSalida.TABLA);
        carpeta.setText(ajustes.carpetaSalida().toString());
        columnaNombre.setValue(perfil != null ? perfil.getColumnaNombre() : null);
        ultimo = null;
        irAPaso(1);
    }

    // ---------- Navegación ----------

    @FXML
    private void siguiente() {
        if (ocupado || !valido(paso)) {
            if (paso == 3) resaltarFaltantes();
            return;
        }
        if (paso < PASOS) {
            irAPaso(paso + 1);
        } else {
            generar();
        }
    }

    @FXML
    private void atras() {
        if (!ocupado && paso > 1 && paso <= PASOS) irAPaso(paso - 1);
    }

    /** Esc o Cancelar: durante la generación la detiene; si no, sale del asistente sin generar nada. */
    @FXML
    private void cancelar() {
        if (ocupado) {
            cancelarGeneracion();
            return;
        }
        if (paso > PASOS) {
            reiniciar(null, null);
            navegador.ir(Seccion.INICIO);
            return;
        }
        if (dialogos.confirmar(navegador.ventana(), Textos.t("generar.salir.titulo"), Textos.t("generar.salir.mensaje"),
                Textos.t("generar.salir.si"))) {
            reiniciar(null, null);
            navegador.ir(Seccion.INICIO);
        }
    }

    @FXML
    private void cancelarGeneracion() {
        pedirCancelar.set(true);
        cancelarGeneracion.setDisable(true);
        progresoTexto.setText(Textos.t("generar.cancelando"));
    }

    private void irAPaso(int n) {
        paso = n;
        switch (n) {
            case 1 -> mostrarDatos();
            case 2 -> prepararPlantillas();
            case 3 -> prepararMapeo();
            case 4 -> prepararSalida();
            default -> {
            }
        }
        mostrarPanel(switch (n) {
            case 1 -> paso1;
            case 2 -> paso2;
            case 3 -> paso3;
            case 4 -> paso4;
            case 5 -> generando;
            default -> resultado;
        });
        botonera.setVisible(n <= PASOS);
        botonera.setManaged(n <= PASOS);
        siguiente.setText(n == PASOS ? Textos.t("generar.boton.generar") : Textos.t("generar.boton.siguiente"));
        marcarIndicador();
        actualizarBotones();
    }

    private void mostrarPanel(Node visible) {
        for (Node p : List.of(paso1, paso2, paso3, paso4, generando, resultado)) {
            p.setVisible(p == visible);
            p.setManaged(p == visible);
        }
    }

    private boolean valido(int p) {
        SesionTrabajo.DatosCargados d = sesion.getDatos();
        return switch (p) {
            case 1 -> d != null && !d.hayErrores() && d.tabla().totalFilas() > 0;
            case 2 -> plantillas.getSelectionModel().getSelectedItem() != null;
            case 3 -> plantillaElegida() != null && GeneradorInforme.faltantes(plantillaElegida(), mapeo).isEmpty();
            case 4 -> !carpeta.getText().isBlank() && (!guardarPerfil.isSelected() || !nombrePerfil.getText().isBlank());
            default -> false;
        };
    }

    private void actualizarBotones() {
        atras.setDisable(paso <= 1 || paso > PASOS);
        siguiente.setDisable(paso > PASOS || !valido(paso));
        if (paso == 3) actualizarEstadoMapeo();
    }

    // ---------- Paso 1: datos ----------

    @FXML
    private void elegirArchivo() {
        FileChooser fc = DatosController.selectorDatos();
        File f = fc.showOpenDialog(navegador.ventana());
        if (f != null) leer(f.toPath(), null);
    }

    @FXML
    private void verDatos() {
        navegador.ir(Seccion.DATOS);
    }

    private void leer(Path archivo, String nombreHoja) {
        Task<SesionTrabajo.DatosCargados> tarea = sesion.tareaLeer(archivo, nombreHoja);
        datosResumen.setText(Textos.t("generar.leyendo"));
        tarea.setOnSucceeded(e -> sesion.datosProperty().set(tarea.getValue()));
        tarea.setOnFailed(e -> {
            mostrarDatos();
            dialogos.error(navegador.ventana(), Textos.t("datos.errorLeer", archivo.getFileName()), tarea.getException());
        });
        SesionTrabajo.ejecutar(tarea, "leer-datos");
    }

    private void mostrarDatos() {
        SesionTrabajo.DatosCargados d = sesion.getDatos();
        boolean hay = d != null;
        cajaHoja.setVisible(hay && !d.hojas().isEmpty());
        cajaHoja.setManaged(cajaHoja.isVisible());
        if (!hay) {
            datosArchivo.setText(Textos.t("generar.paso1.ninguno"));
            datosDetalle.setText(Textos.t("generar.paso1.ayuda"));
            datosResumen.setText("");
            datosErrores.setText("");
            return;
        }
        datosArchivo.setText(d.tabla().nombreArchivo());
        datosDetalle.setText(d.tabla().detalle() + "  ·  " + d.tabla().archivo().getParent());
        if (!d.hojas().equals(hoja.getItems())) hoja.getItems().setAll(d.hojas());
        if (d.tabla().hoja() != null && !d.tabla().hoja().equals(hoja.getValue())) hoja.setValue(d.tabla().hoja());
        datosResumen.setText(TablaVistaPrevia.resumen(d.tabla(), d.problemas()));
        datosErrores.setText(d.hayErrores() ? Textos.t("generar.paso1.hayErrores")
                : d.problemas().isEmpty() ? Textos.t("generar.paso1.sinAvisos") : Textos.t("generar.paso1.conAvisos"));
        datosErrores.getStyleClass().removeAll("texto-error", "texto-ok", "texto-aviso");
        datosErrores.getStyleClass().add(d.hayErrores() ? "texto-error" : d.problemas().isEmpty() ? "texto-ok" : "texto-aviso");
    }

    // ---------- Paso 2: plantilla ----------

    private void prepararPlantillas() {
        Plantilla seleccionada = plantillaElegida();
        Long id = seleccionada != null ? seleccionada.getId() : plantillaInicial != null ? plantillaInicial.getId() : null;
        plantillas.getItems().setAll(servicioPlantillas.listar());
        if (id != null) {
            for (Plantilla p : plantillas.getItems()) {
                if (p.getId().equals(id)) {
                    plantillas.getSelectionModel().select(p);
                    plantillas.scrollTo(p);
                }
            }
        }
        plantillaInicial = null;
        plantillas.requestFocus();
    }

    private Plantilla plantillaElegida() {
        return plantillas.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void irAPlantillas() {
        navegador.ir(Seccion.PLANTILLAS);
    }

    // ---------- Paso 3: mapeo ----------

    private void prepararMapeo() {
        Plantilla p = plantillaElegida();
        TablaDatos t = sesion.getDatos().tabla();
        gridMapeo.getChildren().clear();
        combos.clear();
        etiquetasMapeo.clear();
        List<String> campos = p.camposPorMapear();
        mapeoVacio.setVisible(campos.isEmpty());
        mapeoVacio.setManaged(campos.isEmpty());

        // Opciones: columnas del archivo + valores especiales (fecha de hoy, número de fila, total)
        List<String> opciones = new ArrayList<>(t.columnas());
        opciones.addAll(GeneradorInforme.SEUDO_COLUMNAS);
        // Lo que no exista en este archivo se descarta; lo que falte se sugiere por nombre parecido
        mapeo.entrySet().removeIf(e -> !opciones.contains(e.getValue()) || !campos.contains(e.getKey()));
        List<String> sinColumna = campos.stream().filter(c -> !mapeo.containsKey(c)).toList();
        List<String> libres = t.columnas().stream().filter(c -> !mapeo.containsValue(c)).toList();
        SugeridorMapeo.sugerir(sinColumna, libres).forEach((campo, col) -> {
            if (col != null) mapeo.put(campo, col);
        });

        Label h1 = new Label(Textos.t("generar.mapeo.marcador"));
        Label h2 = new Label(Textos.t("generar.mapeo.columna"));
        h1.getStyleClass().add("encabezado-grid");
        h2.getStyleClass().add("encabezado-grid");
        gridMapeo.addRow(0, h1, h2);
        int fila = 1;
        for (String campo : campos) {
            boolean deTabla = p.listaColumnasTabla().contains(campo) && !p.listaMarcadores().contains(campo);
            String texto = p.getTipo() == TipoPlantilla.DOCX ? (deTabla ? "[" + campo + "]" : "{{" + campo + "}}") : campo;
            Label etiqueta = new Label(texto + (deTabla ? "   " + Textos.t("plantillas.columnaTabla") : ""));
            etiqueta.getStyleClass().add("marcador");
            ComboBox<String> combo = new ComboBox<>();
            combo.getItems().add("");
            combo.getItems().addAll(opciones);
            combo.setConverter(convertidorColumna());
            combo.setPromptText(Textos.t("mapeo.sinColumna"));
            combo.setMaxWidth(Double.MAX_VALUE);
            combo.setPrefWidth(320);
            combo.setValue(mapeo.get(campo));
            combo.valueProperty().addListener((o, a, n) -> {
                if (n == null || n.isEmpty()) mapeo.remove(campo);
                else mapeo.put(campo, n);
                actualizarBotones();
            });
            GridPane.setHgrow(combo, Priority.ALWAYS);
            gridMapeo.addRow(fila++, etiqueta, combo);
            combos.put(campo, combo);
            etiquetasMapeo.put(campo, etiqueta);
        }
        actualizarEstadoMapeo();
    }

    @FXML
    private void sugerirMapeo() {
        TablaDatos t = sesion.getDatos().tabla();
        SugeridorMapeo.sugerir(new ArrayList<>(combos.keySet()), t.columnas()).forEach((campo, col) -> {
            if (col != null) combos.get(campo).setValue(col);
        });
    }

    @FXML
    private void limpiarMapeo() {
        combos.values().forEach(c -> c.setValue(null));
    }

    private void actualizarEstadoMapeo() {
        Plantilla p = plantillaElegida();
        if (p == null) return;
        List<String> faltan = GeneradorInforme.faltantes(p, mapeo);
        etiquetasMapeo.forEach((campo, etiqueta) -> {
            etiqueta.getStyleClass().remove("marcador-falta");
            combos.get(campo).getStyleClass().remove("combo-falta");
        });
        mapeoEstado.getStyleClass().removeAll("texto-error", "texto-ok");
        if (faltan.isEmpty()) {
            mapeoEstado.setText(p.camposPorMapear().isEmpty() ? "" : Textos.t("generar.mapeo.completo"));
            mapeoEstado.getStyleClass().add("texto-ok");
        } else {
            mapeoEstado.setText(Textos.t("generar.mapeo.faltan", faltan.size()));
            mapeoEstado.getStyleClass().add("texto-error");
        }
    }

    /** Resalta en rojo los marcadores sin columna. */
    private void resaltarFaltantes() {
        for (String campo : GeneradorInforme.faltantes(plantillaElegida(), mapeo)) {
            Label e = etiquetasMapeo.get(campo);
            if (e != null && !e.getStyleClass().contains("marcador-falta")) e.getStyleClass().add("marcador-falta");
            ComboBox<String> c = combos.get(campo);
            if (c != null && !c.getStyleClass().contains("combo-falta")) c.getStyleClass().add("combo-falta");
        }
    }

    // ---------- Paso 4: salida ----------

    private void prepararSalida() {
        Plantilla p = plantillaElegida();
        formatoSalida.setText(p.getTipo() == TipoPlantilla.DOCX ? Textos.t("generar.formato.docx") : Textos.t("generar.formato.pdf"));
        TablaDatos t = sesion.getDatos().tabla();
        String actual = columnaNombre.getValue();
        columnaNombre.getItems().setAll("");
        columnaNombre.getItems().addAll(t.columnas());
        columnaNombre.setValue(actual != null && t.columnas().contains(actual) ? actual : null);
        if (nombrePerfil.getText().isBlank()) nombrePerfil.setText(p.getNombre());
        actualizarModo();
    }

    private void actualizarModo() {
        Plantilla p = plantillaElegida();
        boolean porFila = modoPorFila.isSelected();
        cajaNombre.setDisable(!porFila);
        if (p == null) return;
        int filas = sesion.getDatos() == null ? 0 : sesion.getDatos().tabla().totalFilas();
        String aviso;
        if (porFila) {
            aviso = Textos.t("generar.modo.porFilaInfo", filas, p.getTipo().extensionSalida().toUpperCase(Locale.ROOT));
        } else if (p.getTipo() == TipoPlantilla.DOCX && !p.isTieneTabla()) {
            aviso = Textos.t("generar.modo.sinTabla");
        } else {
            aviso = Textos.t("generar.modo.tablaInfo", filas);
        }
        modoAviso.setText(aviso);
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
            actualizarBotones();
        }
    }

    // ---------- Generación ----------

    private void generar() {
        Plantilla p = plantillaElegida();
        ModoSalida modo = modoPorFila.isSelected() ? ModoSalida.POR_FILA : ModoSalida.TABLA;
        String colNombre = modo == ModoSalida.POR_FILA && columnaNombre.getValue() != null && !columnaNombre.getValue().isEmpty()
                ? columnaNombre.getValue() : null;
        Map<String, String> mapeoFinal = new LinkedHashMap<>(mapeo);
        GeneradorInforme.Solicitud solicitud = new GeneradorInforme.Solicitud(
                sesion.getDatos().tabla(), p, mapeoFinal, modo, Path.of(carpeta.getText().trim()), colNombre);

        try {
            if (guardarPerfil.isSelected()) {
                servicioPerfiles.guardar(nombrePerfil.getText(), p, mapeoFinal, modo, colNombre);
            } else if (perfilOrigen != null) {
                servicioPerfiles.marcarUso(perfilOrigen);
            }
        } catch (RuntimeException e) {
            dialogos.error(navegador.ventana(), Textos.t("generar.errorPerfil"), e);
        }

        ocupado = true;
        pedirCancelar.set(false);
        cancelarGeneracion.setDisable(false);
        barra.setProgress(-1);
        progresoTexto.setText(Textos.t("generar.preparando"));
        irAPaso(5);

        Task<GeneradorInforme.Resultado> tarea = new Task<>() {
            @Override
            protected GeneradorInforme.Resultado call() throws Exception {
                long inicio = System.currentTimeMillis();
                GeneradorInforme.Resultado r = generador.generar(solicitud, new GeneradorInforme.Progreso() {
                    @Override
                    public void avanzar(int hechos, int total, String mensaje) {
                        updateProgress(hechos, total);
                        updateMessage(mensaje);
                    }

                    @Override
                    public boolean cancelado() {
                        return pedirCancelar.get();
                    }
                });
                historial.registrar(solicitud, r, System.currentTimeMillis() - inicio);
                return r;
            }
        };
        barra.progressProperty().bind(tarea.progressProperty());
        tarea.messageProperty().addListener((o, a, n) -> {
            if (!pedirCancelar.get() && n != null && !n.isEmpty()) progresoTexto.setText(n);
        });
        tarea.setOnSucceeded(e -> terminar(tarea.getValue(), null));
        tarea.setOnFailed(e -> terminar(null, tarea.getException()));
        SesionTrabajo.ejecutar(tarea, "generar-informes");
    }

    private void terminar(GeneradorInforme.Resultado r, Throwable error) {
        barra.progressProperty().unbind();
        ocupado = false;
        ultimo = r;
        resultadoErrores.getItems().clear();
        resultadoTitulo.getStyleClass().removeAll("texto-error", "texto-ok", "texto-aviso");
        if (error != null) {
            resultadoTitulo.setText(Textos.t("estado." + EstadoEjecucion.FALLIDA.name()));
            resultadoTitulo.getStyleClass().add("texto-error");
            resultadoTexto.setText(Dialogos.mensajeClaro(error));
        } else {
            EstadoEjecucion estado = r.cancelado() ? EstadoEjecucion.CANCELADA
                    : r.errores().isEmpty() ? EstadoEjecucion.COMPLETADA
                    : r.generados() == 0 ? EstadoEjecucion.FALLIDA : EstadoEjecucion.CON_ERRORES;
            resultadoTitulo.setText(Formatos.estado(estado));
            resultadoTitulo.getStyleClass().add(estado == EstadoEjecucion.COMPLETADA ? "texto-ok"
                    : estado == EstadoEjecucion.FALLIDA ? "texto-error" : "texto-aviso");
            resultadoTexto.setText(Textos.t("generar.resultado.texto", r.generados(), r.filasProcesadas(), r.carpeta().toString()));
            resultadoErrores.getItems().setAll(r.errores());
        }
        resultadoErrores.setVisible(!resultadoErrores.getItems().isEmpty());
        resultadoErrores.setManaged(resultadoErrores.isVisible());
        irAPaso(6);
    }

    @FXML
    private void abrirCarpetaResultado() {
        if (ultimo != null && ultimo.carpeta() != null) escritorio.abrir(ultimo.carpeta());
    }

    @FXML
    private void otroReporte() {
        reiniciar(null, null);
    }

    @FXML
    private void irAlHistorial() {
        reiniciar(null, null);
        navegador.ir(Seccion.HISTORIAL);
    }

    // ---------- Apoyo ----------

    private void construirIndicador() {
        indicador.getChildren().clear();
        for (int i = 1; i <= PASOS; i++) {
            Label l = new Label(i + ". " + Textos.t("generar.paso" + i + ".titulo"));
            l.getStyleClass().add("paso");
            indicador.getChildren().add(l);
            if (i < PASOS) {
                Label sep = new Label("›");
                sep.getStyleClass().add("paso-separador");
                indicador.getChildren().add(sep);
            }
        }
    }

    private void marcarIndicador() {
        int i = 1;
        for (Node n : indicador.getChildren()) {
            if (!n.getStyleClass().contains("paso")) continue;
            n.getStyleClass().removeAll("paso-actual", "paso-hecho");
            if (i == paso) n.getStyleClass().add("paso-actual");
            else if (i < paso) n.getStyleClass().add("paso-hecho");
            i++;
        }
    }

    private void registrarAtajos(Scene escena) {
        if (escena == null || atajosRegistrados) return;
        atajosRegistrados = true;
        boolean mac = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
        KeyCombination anterior = mac
                ? new KeyCodeCombination(KeyCode.OPEN_BRACKET, KeyCombination.META_DOWN)
                : new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN);
        escena.getAccelerators().put(anterior, () -> {
            // Solo cuando el asistente está en pantalla
            if (raiz.getScene() != null && raiz.getParent() != null && raiz.isVisible()) atras();
        });
    }

    private static StringConverter<String> convertidorColumna() {
        return new StringConverter<>() {
            @Override
            public String toString(String c) {
                return c == null || c.isEmpty() ? "" : Formatos.columna(c);
            }

            @Override
            public String fromString(String s) {
                return s;
            }
        };
    }
}
