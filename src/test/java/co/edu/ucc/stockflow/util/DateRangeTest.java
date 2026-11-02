package co.edu.ucc.stockflow.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    @Test
    void wholeDaysEndAtMidnightOfTheNextDay() {
        DateRange r = DateRange.ofDays(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        assertEquals(LocalDate.of(2026, 11, 1).atStartOfDay(), r.start());
        assertEquals(LocalDate.of(2026, 12, 1).atStartOfDay(), r.end());
    }

    @Test
    void swapsDatesGivenInReverse() {
        DateRange r = DateRange.ofDays(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1));
        assertEquals(LocalDate.of(2026, 11, 1).atStartOfDay(), r.start());
    }

    @Test
    void lastSevenDaysIncludeToday() {
        DateRange r = DateRange.lastDays(7);
        assertEquals(LocalDate.now().minusDays(6).atStartOfDay(), r.start());
        assertEquals(LocalDate.now().plusDays(1).atStartOfDay(), r.end());
    }
}
