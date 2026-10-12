package co.edu.ucc.reportes.service;

import co.edu.ucc.reportes.i18n.Textos;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/** Lee .csv detectando el separador (, ; tabulador |) y la codificación (UTF-8 o ANSI de Windows). */
public final class LectorCsv {

    static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private static final char[] SEPARADORES = {';', ',', '\t', '|'};

    private LectorCsv() {
    }

    public static TablaDatos leer(Path archivo) throws IOException {
        byte[] bytes = Files.readAllBytes(archivo);
        Charset codificacion = detectarCodificacion(bytes);
        int saltar = tieneBomUtf8(bytes) ? 3 : 0;
        String texto = new String(bytes, saltar, bytes.length - saltar, codificacion);
        char separador = detectarSeparador(texto);

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(separador)
                .setIgnoreEmptyLines(false)
                .build();
        List<List<String>> crudas = new ArrayList<>();
        List<Integer> numeros = new ArrayList<>();
        try (CSVParser parser = formato.parse(new StringReader(texto))) {
            for (CSVRecord r : parser) {
                List<String> fila = new ArrayList<>(r.size());
                r.forEach(fila::add);
                crudas.add(fila);
                // Número de la línea donde empieza el registro (como lo vería el usuario)
                numeros.add((int) r.getRecordNumber());
            }
        }
        String detalle = Textos.t("datos.detalleCsv", nombreSeparador(separador), nombreCodificacion(codificacion));
        return ArmadorTabla.armar(archivo, null, detalle, crudas, numeros);
    }

    /** UTF-8 si los bytes son UTF-8 válido; si no, la codificación ANSI de Excel en Windows. */
    public static Charset detectarCodificacion(byte[] bytes) {
        if (tieneBomUtf8(bytes)) return StandardCharsets.UTF_8;
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return WINDOWS_1252;
        }
    }

    /**
     * Elige el separador que aparece el mismo número de veces (y más de cero) en más líneas,
     * contando solo fuera de comillas. Con empate gana el orden ; , tabulador |.
     */
    public static char detectarSeparador(String texto) {
        List<String> lineas = new ArrayList<>();
        for (String l : texto.split("\r?\n")) {
            if (!l.isBlank()) lineas.add(l);
            if (lineas.size() >= 20) break;
        }
        if (lineas.isEmpty()) return ',';

        char mejor = ',';
        double mejorPuntaje = 0;
        for (char sep : SEPARADORES) {
            Map<Integer, Integer> frecuencia = new HashMap<>();
            for (String l : lineas) {
                int n = contarFueraDeComillas(l, sep);
                if (n > 0) frecuencia.merge(n, 1, Integer::sum);
            }
            if (frecuencia.isEmpty()) continue;
            // Líneas que coinciden con la cantidad más común, + desempate por cantidad de columnas
            Map.Entry<Integer, Integer> moda = frecuencia.entrySet().stream()
                    .max(Map.Entry.<Integer, Integer>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                    .orElseThrow();
            double puntaje = moda.getValue() + Math.min(moda.getKey(), 50) / 100.0;
            if (puntaje > mejorPuntaje) {
                mejorPuntaje = puntaje;
                mejor = sep;
            }
        }
        return mejor;
    }

    static int contarFueraDeComillas(String linea, char sep) {
        boolean comillas = false;
        int n = 0;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '"') comillas = !comillas;
            else if (c == sep && !comillas) n++;
        }
        return n;
    }

    private static boolean tieneBomUtf8(byte[] b) {
        return b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF;
    }

    static String nombreSeparador(char c) {
        return switch (c) {
            case ';' -> Textos.t("separador.puntoComa");
            case '\t' -> Textos.t("separador.tabulador");
            case '|' -> Textos.t("separador.barra");
            default -> Textos.t("separador.coma");
        };
    }

    static String nombreCodificacion(Charset c) {
        return c.equals(StandardCharsets.UTF_8) ? "UTF-8" : "ANSI (Windows-1252)";
    }
}
