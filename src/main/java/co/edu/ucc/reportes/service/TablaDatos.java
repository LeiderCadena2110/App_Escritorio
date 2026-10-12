package co.edu.ucc.reportes.service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Datos leídos de un Excel o CSV: nombres de columna y filas de texto ya formateado.
 * numerosFila guarda el número de fila tal como aparece en el archivo (para los mensajes).
 */
public record TablaDatos(
        Path archivo,
        String hoja,
        String detalle,
        List<String> columnas,
        List<List<String>> filas,
        List<Integer> numerosFila,
        List<Problema> problemasLectura) {

    public int totalFilas() {
        return filas.size();
    }

    public String valor(int fila, String columna) {
        int c = columnas.indexOf(columna);
        if (c < 0) return "";
        List<String> f = filas.get(fila);
        return c < f.size() ? f.get(c) : "";
    }

    public Map<String, String> filaComoMapa(int fila) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String c : columnas) m.put(c, valor(fila, c));
        return m;
    }

    public String nombreArchivo() {
        return archivo == null ? "" : archivo.getFileName().toString();
    }
}
