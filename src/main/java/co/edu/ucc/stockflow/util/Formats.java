package co.edu.ucc.stockflow.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Date formats shown on screen and in files. */
public final class Formats {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private Formats() {
    }

    public static String date(LocalDate d) {
        return d == null ? "" : DATE.format(d);
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? "" : DATE_TIME.format(d);
    }

    public static String blankIfNull(String s) {
        return s == null ? "" : s;
    }

    /** Trims the text and turns "" into null (for optional database fields). */
    public static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
