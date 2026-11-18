package co.edu.ucc.stockflow.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

    @Test
    void periodsFollowTheInjectedClock() {
        // 31 Dec 2026 at 23:30 in Bogotá: "this month" is December, "today" ends at midnight
        Clock lastNightOfYear = Clock.fixed(Instant.parse("2027-01-01T04:30:00Z"), ZoneId.of("America/Bogota"));
        assertEquals(LocalDate.of(2026, 12, 1).atStartOfDay(), DateRange.thisMonth(lastNightOfYear).start());
        assertEquals(LocalDate.of(2027, 1, 1).atStartOfDay(), DateRange.today(lastNightOfYear).end());
        assertEquals(LocalDate.of(2026, 12, 25).atStartOfDay(), DateRange.lastDays(7, lastNightOfYear).start());
    }
}
