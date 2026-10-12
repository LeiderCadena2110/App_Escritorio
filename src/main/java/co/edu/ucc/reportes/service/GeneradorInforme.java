package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.domain.ModoSalida;
import co.edu.ucc.reportes.domain.Plantilla;
import co.edu.ucc.reportes.domain.TipoPlantilla;
import co.edu.ucc.reportes.i18n.Textos;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.deepoove.poi.util.RegexUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Genera los informes: .docx desde plantillas Word (poi-tl) y .pdf desde plantillas HTML.
 * Cada ejecución guarda sus archivos en una subcarpeta nueva dentro de la carpeta de salida.
 */
@Service
public class GeneradorInforme {

    /** Valores que no vienen de una columna: se eligen en el mapeo como columnas especiales. */
    public static final String SEUDO_FECHA = "@fecha";
    public static final String SEUDO_NUMERO = "@numero";
    public static final String SEUDO_TOTAL = "@total";
    public static final List<String> SEUDO_COLUMNAS = List.of(SEUDO_FECHA, SEUDO_NUMERO, SEUDO_TOTAL);

    public record Solicitud(
            TablaDatos tabla,
            Plantilla plantilla,
            Map<String, String> mapeo,
            ModoSalida modo,
            Path carpetaBase,
            String columnaNombre) {
    }

    public record Resultado(
            Path carpeta,
            int generados,
            int filasProcesadas,
            List<String> errores,
            boolean cancelado,
            List<Path> archivos) {
    }

    /** La interfaz recibe el avance y puede pedir cancelar. */
    public interface Progreso {
        void avanzar(int hechos, int total, String mensaje);

        boolean cancelado();
    }

    private final MotorPdf motorPdf;
    private final ServicioAjustes ajustes;

    public GeneradorInforme(MotorPdf motorPdf, ServicioAjustes ajustes) {
        this.motorPdf = motorPdf;
        this.ajustes = ajustes;
    }

    /** Marcadores que todavía no tienen columna. Si la lista está vacía, se puede generar. */
    public static List<String> faltantes(Plantilla p, Map<String, String> mapeo) {
        List<String> f = new ArrayList<>();
        for (String campo : p.camposPorMapear()) {
            String col = mapeo.get(campo);
            if (col == null || col.isBlank()) f.add(campo);
        }
        return f;
    }

    public Resultado generar(Solicitud s, Progreso progreso) throws IOException {
        List<String> faltan = faltantes(s.plantilla(), s.mapeo());
        if (!faltan.isEmpty()) {
            throw new IllegalArgumentException(Textos.t("generar.faltanMarcadores", String.join(", ", faltan)));
        }
        FormatoValores formato = ajustes.formatoValores();
        String hoy = formato.fecha(LocalDate.now().atStartOfDay());
        String sello = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss").format(LocalDateTime.now());
        String nombrePlantilla = NombresArchivo.limpiar(s.plantilla().getNombre(), "informe");
        Path carpeta = s.carpetaBase().resolve(NombresArchivo.limpiar(nombrePlantilla + " " + sello, "informes " + sello));
        Files.createDirectories(carpeta);
        String ext = s.plantilla().getTipo().extensionSalida();

        int total = s.tabla().totalFilas();
        List<String> errores = new ArrayList<>();
        List<Path> archivos = new ArrayList<>();
        boolean cancelado = false;
        int procesadas = 0;

        if (s.modo() == ModoSalida.POR_FILA) {
            for (int i = 0; i < total; i++) {
                if (progreso.cancelado()) {
                    cancelado = true;
                    break;
                }
                int filaArchivo = s.tabla().numerosFila().get(i);
                try {
                    Map<String, String> valores = valores(s, i, hoy);
                    Path destino = NombresArchivo.unico(carpeta, nombreBase(s, i, nombrePlantilla), ext);
                    escribir(s, List.of(valores), List.of(i), valores, hoy, destino, formato);
                    archivos.add(destino);
                } catch (Exception e) {
                    errores.add(Textos.t("generar.errorFila", filaArchivo, mensaje(e)));
                }
                procesadas++;
                progreso.avanzar(i + 1, total, Textos.t("generar.progresoFila", i + 1, total));
            }
        } else {
            List<Map<String, String>> todas = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < total; i++) {
                if (progreso.cancelado()) {
                    cancelado = true;
                    break;
                }
                todas.add(valores(s, i, hoy));
                indices.add(i);
                procesadas++;
                progreso.avanzar(i + 1, total + 1, Textos.t("generar.progresoLeyendo", i + 1, total));
            }
            if (!cancelado) {
                try {
                    Map<String, String> cabecera = todas.isEmpty() ? valoresVacios(s, hoy) : todas.get(0);
                    Path destino = NombresArchivo.unico(carpeta, nombrePlantilla, ext);
                    escribir(s, todas, indices, cabecera, hoy, destino, formato);
                    archivos.add(destino);
                } catch (Exception e) {
                    errores.add(Textos.t("generar.errorTabla", mensaje(e)));
                }
                progreso.avanzar(total + 1, total + 1, Textos.t("generar.progresoListo"));
            }
        }
        return new Resultado(carpeta, archivos.size(), procesadas, errores, cancelado, archivos);
    }

    /** Valores de una fila para cada marcador, según el mapeo. */
    Map<String, String> valores(Solicitud s, int fila, String hoy) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String campo : s.plantilla().camposPorMapear()) {
            String col = s.mapeo().get(campo);
            m.put(campo, valorDe(s.tabla(), col, fila, hoy));
        }
        return m;
    }

    private Map<String, String> valoresVacios(Solicitud s, String hoy) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String campo : s.plantilla().camposPorMapear()) {
            String col = s.mapeo().get(campo);
            m.put(campo, SEUDO_COLUMNAS.contains(col) ? valorDe(s.tabla(), col, 0, hoy) : "");
        }
        return m;
    }

    static String valorDe(TablaDatos tabla, String columna, int fila, String hoy) {
        if (columna == null) return "";
        return switch (columna) {
            case SEUDO_FECHA -> hoy;
            case SEUDO_NUMERO -> Integer.toString(fila + 1);
            case SEUDO_TOTAL -> Integer.toString(tabla.totalFilas());
            default -> tabla.valor(fila, columna);
        };
    }

    private String nombreBase(Solicitud s, int fila, String nombrePlantilla) {
        String porDefecto = nombrePlantilla + " " + String.format("%04d", fila + 1);
        if (s.columnaNombre() == null || s.columnaNombre().isBlank()) return porDefecto;
        String v = valorDe(s.tabla(), s.columnaNombre(), fila, "");
        return NombresArchivo.limpiar(v, porDefecto);
    }

    private void escribir(Solicitud s, List<Map<String, String>> filas, List<Integer> indices, Map<String, String> cabecera,
                          String hoy, Path destino, FormatoValores formato) throws IOException {
        Plantilla p = s.plantilla();
        if (p.getTipo() == TipoPlantilla.DOCX) {
            Map<String, Object> datos = new HashMap<>(cabecera);
            datos.put(DetectorMarcadores.TABLA, filas);
            escribirDocx(Path.of(p.getRuta()), datos, destino);
        } else {
            Map<String, Object> variables = new HashMap<>();
            variables.put("filas", filas);
            variables.put("columnas", s.tabla().columnas());
            List<List<String>> registros = new ArrayList<>();
            for (int i : indices) registros.add(s.tabla().filas().get(i));
            variables.put("registros", registros);
            variables.put("titulo", p.getNombre());
            variables.put("archivo", s.tabla().nombreArchivo().replaceFirst("\\.[^.]+$", ""));
            variables.put("fecha", hoy);
            variables.put("total", filas.size());
            motorPdf.generar(p.getRuta(), variables, formato.idioma(), destino);
        }
    }

    static void escribirDocx(Path plantilla, Map<String, Object> datos, Path destino) throws IOException {
        // Marcadores con tildes o espacios ({{dirección}}) y la tabla {{filas}} que repite filas [columna]
        Configure config = Configure.builder()
                .buildGrammerRegex(RegexUtils.createGeneral("{{", "}}"))
                .bind(DetectorMarcadores.TABLA, new LoopRowTableRenderPolicy())
                .build();
        try (InputStream in = Files.newInputStream(plantilla); OutputStream out = Files.newOutputStream(destino)) {
            XWPFTemplate.compile(in, config).render(datos).writeAndClose(out);
        }
    }

    private static String mensaje(Exception e) {
        Throwable t = e;
        while (t.getCause() != null && (t.getMessage() == null || t.getMessage().isBlank())) t = t.getCause();
        String m = t.getMessage();
        return m == null || m.isBlank() ? t.getClass().getSimpleName() : m;
    }
}
