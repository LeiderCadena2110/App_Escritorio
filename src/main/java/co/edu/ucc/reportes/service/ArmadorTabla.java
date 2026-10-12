package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.i18n.Textos;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convierte filas "crudas" en una TablaDatos: la primera fila con contenido es el encabezado,
 * se saltan filas vacías, se nombran columnas sin título y se distinguen nombres repetidos.
 */
final class ArmadorTabla {

    private ArmadorTabla() {
    }

    static TablaDatos armar(Path archivo, String hoja, String detalle, List<List<String>> crudas, List<Integer> numeros) {
        List<Problema> problemas = new ArrayList<>();
        int inicio = 0;
        while (inicio < crudas.size() && vacia(crudas.get(inicio))) inicio++;
        if (inicio >= crudas.size()) {
            problemas.add(Problema.error(null, null, Textos.t("validacion.archivoVacio")));
            return new TablaDatos(archivo, hoja, detalle, List.of(), List.of(), List.of(), problemas);
        }

        List<String> encabezado = crudas.get(inicio);
        // El ancho real: hasta la última columna con algún dato o título
        int ancho = ultimaConDato(encabezado);
        for (int i = inicio + 1; i < crudas.size(); i++) ancho = Math.max(ancho, ultimaConDato(crudas.get(i)));

        List<String> columnas = new ArrayList<>();
        Set<String> usados = new HashSet<>();
        int filaEncabezado = numeros.get(inicio);
        for (int c = 0; c < ancho; c++) {
            String nombre = c < encabezado.size() ? encabezado.get(c).trim() : "";
            if (nombre.isEmpty()) {
                nombre = Textos.t("validacion.columnaGenerica", c + 1);
                problemas.add(Problema.aviso(filaEncabezado, nombre, Textos.t("validacion.columnaSinNombre", c + 1, nombre)));
            }
            String base = nombre;
            int n = 2;
            while (!usados.add(nombre.toLowerCase())) {
                nombre = base + " (" + n++ + ")";
            }
            if (!nombre.equals(base)) {
                problemas.add(Problema.aviso(filaEncabezado, nombre, Textos.t("validacion.columnaRepetida", base, nombre)));
            }
            columnas.add(nombre);
        }

        List<List<String>> filas = new ArrayList<>();
        List<Integer> nums = new ArrayList<>();
        for (int i = inicio + 1; i < crudas.size(); i++) {
            List<String> f = crudas.get(i);
            if (vacia(f)) continue;
            List<String> fila = new ArrayList<>(ancho);
            for (int c = 0; c < ancho; c++) fila.add(c < f.size() && f.get(c) != null ? f.get(c).trim() : "");
            filas.add(fila);
            nums.add(numeros.get(i));
        }
        if (filas.isEmpty()) {
            problemas.add(Problema.error(null, null, Textos.t("validacion.sinFilas")));
        }
        return new TablaDatos(archivo, hoja, detalle, List.copyOf(columnas), filas, nums, problemas);
    }

    private static boolean vacia(List<String> fila) {
        for (String v : fila) if (v != null && !v.isBlank()) return false;
        return true;
    }

    private static int ultimaConDato(List<String> fila) {
        for (int c = fila.size() - 1; c >= 0; c--) {
            if (fila.get(c) != null && !fila.get(c).isBlank()) return c + 1;
        }
        return 0;
    }
}
