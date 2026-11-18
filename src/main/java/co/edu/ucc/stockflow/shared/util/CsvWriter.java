package co.edu.ucc.stockflow.shared.util;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Writes CSV that Excel opens directly: UTF-8 with BOM (for accents), semicolon as separator and
 * comma as decimal separator, as Excel expects in Spanish. Each module defines its own columns.
 */
public final class CsvWriter {

    private static final char SEPARATOR = ';';

    /** A CSV column: its title and how to get the value from a row. */
    public record Column<T>(String title, Function<T, Object> value) {
    }

    private CsvWriter() {
    }

    public static <T> void write(Path file, List<T> rows, List<Column<T>> columns) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write('\uFEFF');
            List<String> titles = new ArrayList<>();
            for (Column<T> c : columns) titles.add(c.title());
            writeLine(w, titles);
            for (T row : rows) {
                List<String> values = new ArrayList<>();
                for (Column<T> c : columns) values.add(toText(c.value().apply(row)));
                writeLine(w, values);
            }
        }
    }

    static String toText(Object value) {
        if (value == null) return "";
        if (value instanceof BigDecimal d) return d.toPlainString().replace('.', ',');
        return value.toString();
    }

    /** Quotes the value when it has the separator, quotes or line breaks. */
    static String escape(String value) {
        if (value.indexOf(SEPARATOR) >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static void writeLine(Writer w, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) w.write(SEPARATOR);
            w.write(escape(values.get(i)));
        }
        w.write("\r\n");
    }
}
