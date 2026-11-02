package co.edu.ucc.stockflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Sales of one day: number of invoices and total sold. */
public record DailySales(LocalDate date, int invoices, BigDecimal total) {
}
