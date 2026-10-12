package co.edu.ucc.reportes.service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sugiere qué columna va con cada marcador comparando nombres parecidos:
 * "{{nombre_cliente}}" con "Nombre del cliente", "{{telefono}}" con "Teléfono".
 */
public final class SugeridorMapeo {

    /** Parecido mínimo (0 a 1) para sugerir. */
    static final double UMBRAL = 0.6;

    private SugeridorMapeo() {
    }

    /** Devuelve marcador → columna sugerida (o null). Cada columna se sugiere una sola vez. */
    public static Map<String, String> sugerir(List<String> campos, List<String> columnas) {
        Map<String, String> resultado = new LinkedHashMap<>();
        java.util.Set<String> usadas = new java.util.HashSet<>();
        for (String campo : campos) {
            String mejor = null;
            double mejorPuntaje = UMBRAL;
            for (String col : columnas) {
                if (usadas.contains(col)) continue;
                double p = parecido(campo, col);
                if (p > mejorPuntaje || (p == 1.0 && mejorPuntaje < 1.0)) {
                    mejorPuntaje = p;
                    mejor = col;
                }
            }
            resultado.put(campo, mejor);
            if (mejor != null) usadas.add(mejor);
        }
        return resultado;
    }

    /** 1 = mismo nombre (sin tildes, mayúsculas ni signos); 0 = nada en común. */
    public static double parecido(String a, String b) {
        String x = normalizar(a);
        String y = normalizar(b);
        if (x.isEmpty() || y.isEmpty()) return 0;
        if (x.equals(y)) return 1.0;
        String xs = sinRelleno(x);
        String ys = sinRelleno(y);
        if (xs.equals(ys)) return 0.95;
        if (xs.length() >= 3 && ys.length() >= 3 && (xs.contains(ys) || ys.contains(xs))) {
            return 0.75 + 0.2 * Math.min(xs.length(), ys.length()) / Math.max(xs.length(), ys.length());
        }
        int d = levenshtein(xs, ys);
        return 1.0 - (double) d / Math.max(xs.length(), ys.length());
    }

    /** "Teléfono del Cliente" → "telefono del cliente" (sin tildes, _ y - como espacio). */
    public static String normalizar(String s) {
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** Quita palabras de relleno y espacios: "nombre del cliente" → "nombrecliente". */
    private static String sinRelleno(String s) {
        return s.replaceAll("\\b(de|del|la|las|el|los|of|the)\\b", "").replace(" ", "");
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + costo);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
