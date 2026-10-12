package co.edu.ucc.reportes.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

/**
 * Encuentra los marcadores de una plantilla.
 * Word: {{cliente}} en cualquier parte; {{filas}} marca la tabla que se repite y sus
 * celdas usan [columna]. HTML: los campos se escriben como fila['cliente'].
 */
public final class DetectorMarcadores {

    /** Nombre reservado del marcador de la tabla que se repite (modo "todas las filas"). */
    public static final String TABLA = "filas";

    private static final Pattern LLAVES = Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*\\}\\}");
    private static final Pattern CORCHETES = Pattern.compile("\\[\\s*([^\\[\\]{}]+?)\\s*\\]");
    private static final Pattern HTML = Pattern.compile("fila\\[\\s*'([^']+)'\\s*\\]");

    private DetectorMarcadores() {
    }

    /** textos: todos los párrafos; textosTabla: el texto de las celdas de tablas. */
    public static Marcadores detectarEnTextos(List<String> textos, List<String> textosTabla) {
        Set<String> simples = new LinkedHashSet<>();
        boolean tieneTabla = false;
        for (String t : textos) {
            Matcher m = LLAVES.matcher(t);
            while (m.find()) {
                String nombre = m.group(1).trim();
                if (nombre.equalsIgnoreCase(TABLA)) {
                    tieneTabla = true;
                } else if (!nombre.isEmpty() && !"#@*?+/>=".contains(nombre.substring(0, 1))) {
                    simples.add(nombre);
                }
            }
        }
        Set<String> columnas = new LinkedHashSet<>();
        if (tieneTabla) {
            for (String t : textosTabla) {
                Matcher m = CORCHETES.matcher(t);
                while (m.find()) columnas.add(m.group(1).trim());
            }
        }
        return new Marcadores(new ArrayList<>(simples), new ArrayList<>(columnas), tieneTabla);
    }

    public static Marcadores detectarHtml(String html) {
        Set<String> campos = new LinkedHashSet<>();
        Matcher m = HTML.matcher(html);
        while (m.find()) campos.add(m.group(1).trim());
        return new Marcadores(new ArrayList<>(campos), List.of(), false);
    }

    public static Marcadores detectarDocx(Path archivo) throws IOException {
        try (InputStream in = Files.newInputStream(archivo); XWPFDocument doc = new XWPFDocument(in)) {
            List<String> textos = new ArrayList<>();
            List<String> textosTabla = new ArrayList<>();
            recorrer(doc.getBodyElements(), textos, textosTabla);
            for (XWPFHeader h : doc.getHeaderList()) recorrer(h.getBodyElements(), textos, textosTabla);
            for (XWPFFooter f : doc.getFooterList()) recorrer(f.getBodyElements(), textos, textosTabla);
            return detectarEnTextos(textos, textosTabla);
        }
    }

    private static void recorrer(List<IBodyElement> elementos, List<String> textos, List<String> textosTabla) {
        for (IBodyElement e : elementos) {
            if (e instanceof XWPFParagraph p) {
                textos.add(p.getText());
            } else if (e instanceof XWPFTable t) {
                recorrerTabla(t, textos, textosTabla);
            }
        }
    }

    private static void recorrerTabla(XWPFTable tabla, List<String> textos, List<String> textosTabla) {
        for (XWPFTableRow fila : tabla.getRows()) {
            for (XWPFTableCell celda : fila.getTableCells()) {
                for (XWPFParagraph p : celda.getParagraphs()) {
                    textos.add(p.getText());
                    textosTabla.add(p.getText());
                }
                for (XWPFTable interna : celda.getTables()) recorrerTabla(interna, textos, textosTabla);
            }
        }
    }
}
