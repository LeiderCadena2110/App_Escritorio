package co.edu.ucc.stockflow.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Search the way a cashier types: without caring about accents, upper case or word order.
 * "jose per" finds "Pérez, José" and "zapatilla nike" finds "Nike Zapatilla Running".
 */
public final class TextSearch {

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private TextSearch() {
    }

    /** Lower case and without accents ("Peña José" → "pena jose"); null becomes "". */
    public static String normalize(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return MARKS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT).trim();
    }

    /**
     * True when every word of the query appears in at least one of the fields.
     * An empty query matches everything.
     */
    public static boolean matches(String query, String... fields) {
        String q = normalize(query);
        if (q.isEmpty()) return true;
        StringBuilder all = new StringBuilder();
        for (String f : fields) all.append(normalize(f)).append(' ');
        String haystack = all.toString();
        for (String word : SPACES.split(q)) {
            if (!haystack.contains(word)) return false;
        }
        return true;
    }
}
