package co.edu.ucc.stockflow.sales.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CashSuggestionsTest {

    private static List<BigDecimal> amounts(long... values) {
        return java.util.Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }

    @Test
    void exactTotalThenTheNextBills() {
        assertEquals(amounts(37_400, 38_000, 40_000, 50_000), CashSuggestions.forTotal(new BigDecimal("37400")));
    }

    @Test
    void roundAmountsDoNotRepeat() {
        assertEquals(amounts(50_000, 60_000, 100_000), CashSuggestions.forTotal(new BigDecimal("50000")));
        assertEquals(amounts(20_000, 50_000, 100_000), CashSuggestions.forTotal(new BigDecimal("20000")));
    }

    @Test
    void smallBillsAreSkippedForLargeTotals() {
        assertEquals(amounts(248_472, 250_000, 260_000, 300_000), CashSuggestions.forTotal(new BigDecimal("248472")));
        assertEquals(amounts(296_072, 300_000), CashSuggestions.forTotal(new BigDecimal("296072")));
    }

    @Test
    void centsAreRoundedUpSoTheExactButtonCoversTheTotal() {
        assertEquals(BigDecimal.valueOf(18_901), CashSuggestions.forTotal(new BigDecimal("18900.40")).get(0));
    }

    @Test
    void nothingToSuggestForAnEmptySale() {
        assertTrue(CashSuggestions.forTotal(BigDecimal.ZERO).isEmpty());
        assertTrue(CashSuggestions.forTotal(null).isEmpty());
    }
}
