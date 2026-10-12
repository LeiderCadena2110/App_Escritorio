package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.i18n.Textos;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Lee Excel o CSV según la extensión, con los formatos de la configuración. */
@Service
public class LectorDatos {

    private final ServicioAjustes ajustes;

    public LectorDatos(ServicioAjustes ajustes) {
        this.ajustes = ajustes;
    }

    public static boolean esExcel(Path archivo) {
        String n = archivo.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".xlsx") || n.endsWith(".xls") || n.endsWith(".xlsm");
    }

    public static boolean esCsv(Path archivo) {
        String n = archivo.getFileName().toString().toLowerCase(Locale.ROOT);
        return n.endsWith(".csv") || n.endsWith(".txt");
    }

    public List<String> hojas(Path archivo) throws IOException {
        return esExcel(archivo) ? LectorExcel.hojas(archivo) : List.of();
    }

    public TablaDatos leer(Path archivo, String hoja) throws IOException {
        if (esExcel(archivo)) return LectorExcel.leer(archivo, hoja, ajustes.formatoValores());
        if (esCsv(archivo)) return LectorCsv.leer(archivo);
        throw new IOException(Textos.t("datos.formatoNoSoportado", archivo.getFileName()));
    }
}
