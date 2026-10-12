package co.edu.ucc.reportes.service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.Normalizer;

/** Nombres de archivo válidos en Windows, macOS y Linux. */
public final class NombresArchivo {

    private NombresArchivo() {
    }

    /** Quita caracteres prohibidos ( \ / : * ? " < > | ), recorta y limita a 80 caracteres. */
    public static String limpiar(String texto, String siVacio) {
        if (texto == null) return siVacio;
        String s = texto.replaceAll("[\\\\/]", "-").replaceAll("[:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        // Windows no acepta nombres que terminan en punto o espacio
        s = s.replaceAll("[. ]+$", "");
        if (s.length() > 80) s = s.substring(0, 80).trim();
        if (s.isEmpty() || s.matches("(?i)(con|prn|aux|nul|com\\d|lpt\\d)")) return siVacio;
        return s;
    }

    /** carpeta/base.ext, o carpeta/base (2).ext si ya existe. */
    public static Path unico(Path carpeta, String base, String extension) {
        try {
            carpeta.resolve(base + "." + extension);
        } catch (InvalidPathException e) {
            // Sistema sin UTF-8 en los nombres de archivo: se quitan tildes y eñes
            base = Normalizer.normalize(base, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^\\x20-\\x7E]", "_");
        }
        Path p = carpeta.resolve(base + "." + extension);
        int n = 2;
        while (Files.exists(p)) {
            p = carpeta.resolve(base + " (" + n++ + ")." + extension);
        }
        return p;
    }
}
