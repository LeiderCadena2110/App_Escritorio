package co.edu.ucc.stockflow.shared.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/** Period of whole days [start, end) used by searches and reports. */
public record DateRange(LocalDateTime start, LocalDateTime end) {

    /** From the first day at 00:00 to the day after the last one at 00:00. */
    public static DateRange ofDays(LocalDate first, LocalDate last) {
        if (last.isBefore(first)) {
            LocalDate tmp = first;
            first = last;
            last = tmp;
        }
        return new DateRange(first.atStartOfDay(), last.plusDays(1).atStartOfDay());
    }

    public static DateRange today() {
        return today(Clock.systemDefaultZone());
    }

    public static DateRange today(Clock clock) {
        LocalDate d = LocalDate.now(clock);
        return ofDays(d, d);
    }

    public static DateRange thisMonth() {
        return thisMonth(Clock.systemDefaultZone());
    }

    public static DateRange thisMonth(Clock clock) {
        YearMonth m = YearMonth.now(clock);
        return ofDays(m.atDay(1), m.atEndOfMonth());
    }

    public static DateRange lastDays(int days) {
        return lastDays(days, Clock.systemDefaultZone());
    }

    /** The last {@code days} days, today included. */
    public static DateRange lastDays(int days, Clock clock) {
        LocalDate d = LocalDate.now(clock);
        return ofDays(d.minusDays(days - 1L), d);
    }
}
