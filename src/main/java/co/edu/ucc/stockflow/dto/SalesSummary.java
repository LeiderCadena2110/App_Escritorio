package co.edu.ucc.stockflow.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Totals of the paid invoices in a period. Profit = (subtotal - discount) - cost of the units sold. */
public record SalesSummary(int invoices, int units, BigDecimal subtotal, BigDecimal discount, BigDecimal tax,
                           BigDecimal total, BigDecimal cost) {

    public static SalesSummary empty() {
        return new SalesSummary(0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO);
    }

    public BigDecimal profit() {
        return subtotal.subtract(discount).subtract(cost);
    }

    public BigDecimal averageTicket() {
        if (invoices == 0) return BigDecimal.ZERO;
        return total.divide(BigDecimal.valueOf(invoices), 0, RoundingMode.HALF_UP);
    }
}
