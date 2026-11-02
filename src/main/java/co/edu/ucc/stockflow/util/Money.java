package co.edu.ucc.stockflow.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.regex.Pattern;

/**
 * Money helpers using the Colombian format: "$ 1.234.567" (dot for thousands, comma for decimals).
 * Decimals are only shown when the value has cents.
 */
public final class Money {

    public static final int SCALE = 2;

    private static final Pattern DOT_THOUSANDS = Pattern.compile("\\d{1,3}(\\.\\d{3})+");
    private static final Pattern COMMA_THOUSANDS = Pattern.compile("\\d{1,3}(,\\d{3})+");

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** "$ 1.234.567" or "$ 1.234,50". */
    public static String format(BigDecimal value) {
        if (value == null) return "";
        return "$ " + number(value);
    }

    /** "1.234.567" or "1.234,50" (no currency sign), useful for text fields. */
    public static String number(BigDecimal value) {
        if (value == null) return "";
        BigDecimal v = round(value);
        boolean hasCents = v.remainder(BigDecimal.ONE).signum() != 0;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        symbols.setMinusSign('-');
        DecimalFormat format = new DecimalFormat(hasCents ? "#,##0.00" : "#,##0", symbols);
        return format.format(v);
    }

    /**
     * Reads what the user typed: "12000", "12.000", "12,000", "$ 12.000,50", "12000.5".
     *
     * @throws NumberFormatException when the text is not a number
     */
    public static BigDecimal parse(String text) {
        if (text == null) throw new NumberFormatException("empty");
        String s = text.replace("$", "").replace(" ", "").replace(" ", "").trim();
        if (s.isEmpty()) throw new NumberFormatException("empty");
        boolean negative = s.startsWith("-");
        if (negative) s = s.substring(1);
        int lastDot = s.lastIndexOf('.');
        int lastComma = s.lastIndexOf(',');
        if (lastDot >= 0 && lastComma >= 0) {
            // Both separators: the last one is the decimal separator
            if (lastComma > lastDot) s = s.replace(".", "").replace(',', '.');
            else s = s.replace(",", "");
        } else if (lastDot >= 0) {
            if (DOT_THOUSANDS.matcher(s).matches()) s = s.replace(".", "");
        } else if (lastComma >= 0) {
            s = COMMA_THOUSANDS.matcher(s).matches() ? s.replace(",", "") : s.replace(',', '.');
        }
        BigDecimal value = new BigDecimal(s);
        return round(negative ? value.negate() : value);
    }

    /** Like {@link #parse(String)} but returns the default when the text is empty or invalid. */
    public static BigDecimal parseOr(String text, BigDecimal defaultValue) {
        try {
            return parse(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** value * percent / 100, rounded. */
    public static BigDecimal percentOf(BigDecimal value, BigDecimal percent) {
        return round(value.multiply(percent).divide(BigDecimal.valueOf(100), SCALE + 2, RoundingMode.HALF_UP));
    }
}
