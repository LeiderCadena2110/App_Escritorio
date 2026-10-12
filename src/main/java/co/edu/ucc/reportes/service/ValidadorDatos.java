package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.i18n.Textos;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Revisa los datos antes de generar: columnas vacías, celdas vacías y valores que no tienen
 * el formato del resto de su columna (por ejemplo, texto en una columna de números).
 * Los mensajes dicen la fila y la columna como se ven en el archivo.
 */
public final class ValidadorDatos {

    /** Máximo de avisos por columna, para no llenar la pantalla. */
    static final int MAX_POR_COLUMNA = 5;

    private static final Pattern NUMERO = Pattern.compile("^[-+]?[$€]?\\s?(\\d{1,3}([.,\\s]\\d{3})+|\\d+)([.,]\\d+)?\\s?%?$");
    private static final Pattern FECHA = Pattern.compile(
            "^(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}|\\d{4}-\\d{1,2}-\\d{1,2})(\\s+\\d{1,2}:\\d{2}(:\\d{2})?)?$");

    private ValidadorDatos() {
    }

    public static List<Problema> validar(TablaDatos tabla) {
        List<Problema> problemas = new ArrayList<>(tabla.problemasLectura());
        for (int c = 0; c < tabla.columnas().size(); c++) {
            String col = tabla.columnas().get(c);
            int vacias = 0;
            int numeros = 0;
            int fechas = 0;
            List<Integer> filasVacias = new ArrayList<>();
            for (int f = 0; f < tabla.totalFilas(); f++) {
                String v = valor(tabla, f, c);
                if (v.isEmpty()) {
                    vacias++;
                    if (filasVacias.size() < MAX_POR_COLUMNA) filasVacias.add(tabla.numerosFila().get(f));
                } else if (esNumero(v)) {
                    numeros++;
                } else if (esFecha(v)) {
                    fechas++;
                }
            }
            int conDato = tabla.totalFilas() - vacias;
            if (tabla.totalFilas() > 0 && conDato == 0) {
                problemas.add(Problema.aviso(null, col, Textos.t("validacion.columnaVacia", col)));
                continue;
            }
            if (vacias > 0) {
                problemas.add(Problema.aviso(filasVacias.get(0), col,
                        Textos.t("validacion.celdasVacias", col, vacias, unir(filasVacias, vacias > filasVacias.size()))));
            }
            // Si casi toda la columna es numérica (o de fechas), lo que no lo es probablemente está mal
            boolean columnaNumerica = conDato >= 3 && numeros >= conDato * 0.75 && numeros < conDato;
            boolean columnaFechas = conDato >= 3 && fechas >= conDato * 0.75 && fechas < conDato;
            if (columnaNumerica || columnaFechas) {
                int reportados = 0;
                for (int f = 0; f < tabla.totalFilas() && reportados < MAX_POR_COLUMNA; f++) {
                    String v = valor(tabla, f, c);
                    if (v.isEmpty()) continue;
                    boolean malo = columnaNumerica ? !esNumero(v) : !esFecha(v);
                    if (malo) {
                        int fila = tabla.numerosFila().get(f);
                        String clave = columnaNumerica ? "validacion.noEsNumero" : "validacion.noEsFecha";
                        problemas.add(Problema.aviso(fila, col, Textos.t(clave, fila, col, v)));
                        reportados++;
                    }
                }
            }
        }
        return problemas;
    }

    public static boolean hayErrores(List<Problema> problemas) {
        return problemas.stream().anyMatch(p -> p.gravedad() == Gravedad.ERROR);
    }

    static boolean esNumero(String v) {
        return NUMERO.matcher(v.trim()).matches();
    }

    static boolean esFecha(String v) {
        return FECHA.matcher(v.trim()).matches();
    }

    private static String valor(TablaDatos t, int f, int c) {
        List<String> fila = t.filas().get(f);
        return c < fila.size() ? fila.get(c).trim() : "";
    }

    private static String unir(List<Integer> filas, boolean hayMas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filas.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(filas.get(i));
        }
        if (hayMas) sb.append("…");
        return sb.toString();
    }
}
