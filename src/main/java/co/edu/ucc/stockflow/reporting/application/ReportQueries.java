package co.edu.ucc.stockflow.reporting.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Read-only sales queries for the dashboard and reports (only PAID invoices count; dates are [from, to)).
 *
 * <p>Port: the application layer depends on this interface and JdbcReportQueries
 * (infrastructure) implements it with SQL, so tests can use an in-memory version instead.</p>
 */
public interface ReportQueries {

    SalesSummary salesSummary(LocalDateTime from, LocalDateTime to);

    List<TopProduct> topProducts(LocalDateTime from, LocalDateTime to, int limit);

    List<DailySales> dailySales(LocalDateTime from, LocalDateTime to);

    /** Units sold of each product in the period, by product id (products without sales are absent). */
    Map<Long, Integer> unitsSoldByProduct(LocalDateTime from, LocalDateTime to);
}
