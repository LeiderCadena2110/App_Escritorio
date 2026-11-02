package co.edu.ucc.stockflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void formatsWithColombianSeparators() {
        assertEquals("$ 1.234.567", Money.format(new BigDecimal("1234567")));
        assertEquals("$ 1.234,50", Money.format(new BigDecimal("1234.5")));
        assertEquals("$ 0", Money.format(BigDecimal.ZERO));
        assertEquals("$ -5.000", Money.format(new BigDecimal("-5000")));
    }

    @Test
    void parsesWhatUsersType() {
        assertEquals(new BigDecimal("12000.00"), Money.parse("12000"));
        assertEquals(new BigDecimal("12000.00"), Money.parse("12.000"));
        assertEquals(new BigDecimal("12000.00"), Money.parse("12,000"));
        assertEquals(new BigDecimal("12000.50"), Money.parse("$ 12.000,50"));
        assertEquals(new BigDecimal("12000.50"), Money.parse("12,000.50"));
        assertEquals(new BigDecimal("12.50"), Money.parse("12.5"));
        assertEquals(new BigDecimal("12.50"), Money.parse("12,5"));
        assertEquals(new BigDecimal("1234567.00"), Money.parse("1.234.567"));
    }

    @Test
    void rejectsInvalidText() {
        assertThrows(NumberFormatException.class, () -> Money.parse(""));
        assertThrows(NumberFormatException.class, () -> Money.parse("abc"));
        assertEquals(BigDecimal.ONE, Money.parseOr("abc", BigDecimal.ONE));
    }

    @Test
    void calculatesPercentages() {
        assertEquals(new BigDecimal("19000.00"), Money.percentOf(new BigDecimal("100000"), new BigDecimal("19")));
        assertEquals(new BigDecimal("0.19"), Money.percentOf(BigDecimal.ONE, new BigDecimal("19")));
    }
}
