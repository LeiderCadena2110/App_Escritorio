package co.edu.ucc.stockflow.sales.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Amounts a customer is likely to pay in cash, shown as quick buttons next to "Recibido".
 *
 * <p>The exact total first, then the total rounded up to the Colombian bills (1.000 … 100.000).
 * Bills too small for the total are skipped, so the buttons are amounts people really hand over:
 * $ 37.400 → 37.400, 38.000, 40.000, 50.000; $ 248.472 → 248.472, 250.000, 260.000, 300.000.</p>
 */
public final class CashSuggestions {

    /** How many buttons fit in the panel. */
    public static final int MAX = 4;

    /** A bill is suggested only if it is at least 1/50 of the total (no 1.000 steps on $ 300.000). */
    private static final int SMALLEST_BILL_RATIO = 50;

    private static final long[] STEPS = {1_000, 2_000, 5_000, 10_000, 20_000, 50_000, 100_000};

    private CashSuggestions() {
    }

    public static List<BigDecimal> forTotal(BigDecimal total) {
        List<BigDecimal> amounts = new ArrayList<>();
        if (total == null || total.signum() <= 0) return amounts;
        BigDecimal exact = total.setScale(0, RoundingMode.CEILING);
        amounts.add(exact);
        for (long step : STEPS) {
            BigDecimal size = BigDecimal.valueOf(step);
            if (size.multiply(BigDecimal.valueOf(SMALLEST_BILL_RATIO)).compareTo(exact) < 0) continue;
            BigDecimal rounded = exact.divide(size, 0, RoundingMode.CEILING).multiply(size);
            if (amounts.size() < MAX && rounded.compareTo(amounts.get(amounts.size() - 1)) > 0) amounts.add(rounded);
        }
        return amounts;
    }
}
