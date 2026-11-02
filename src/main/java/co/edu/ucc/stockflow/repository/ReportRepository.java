package co.edu.ucc.stockflow.repository;

import co.edu.ucc.stockflow.dto.DailySales;
import co.edu.ucc.stockflow.dto.SalesSummary;
import co.edu.ucc.stockflow.dto.TopProduct;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Aggregated sales queries. Only PAID invoices count; dates are [from, to). */
@Repository
public class ReportRepository {

    private final JdbcTemplate jdbc;

    public ReportRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SalesSummary salesSummary(LocalDateTime from, LocalDateTime to) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS invoices,
                       COALESCE(SUM(subtotal), 0) AS subtotal,
                       COALESCE(SUM(discount), 0) AS discount,
                       COALESCE(SUM(tax), 0) AS tax,
                       COALESCE(SUM(total), 0) AS total,
                       COALESCE((SELECT SUM(it.quantity) FROM invoice_items it JOIN invoices iv ON iv.id = it.invoice_id
                                 WHERE iv.status = 'PAID' AND iv.issued_at >= ? AND iv.issued_at < ?), 0) AS units,
                       COALESCE((SELECT SUM(it.unit_cost * it.quantity) FROM invoice_items it
                                 JOIN invoices iv ON iv.id = it.invoice_id
                                 WHERE iv.status = 'PAID' AND iv.issued_at >= ? AND iv.issued_at < ?), 0) AS cost
                FROM invoices
                WHERE status = 'PAID' AND issued_at >= ? AND issued_at < ?
                """,
                (rs, n) -> new SalesSummary(rs.getInt("invoices"), rs.getInt("units"), rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("discount"), rs.getBigDecimal("tax"), rs.getBigDecimal("total"),
                        rs.getBigDecimal("cost")),
                JdbcHelper.timestamp(from), JdbcHelper.timestamp(to),
                JdbcHelper.timestamp(from), JdbcHelper.timestamp(to),
                JdbcHelper.timestamp(from), JdbcHelper.timestamp(to));
    }

    public List<TopProduct> topProducts(LocalDateTime from, LocalDateTime to, int limit) {
        return jdbc.query("""
                SELECT it.product_sku, it.product_name, SUM(it.quantity) AS quantity,
                       SUM(it.line_total) AS revenue,
                       SUM(it.line_total - it.unit_cost * it.quantity) AS profit
                FROM invoice_items it JOIN invoices iv ON iv.id = it.invoice_id
                WHERE iv.status = 'PAID' AND iv.issued_at >= ? AND iv.issued_at < ?
                GROUP BY it.product_sku, it.product_name
                ORDER BY quantity DESC, revenue DESC
                LIMIT ?
                """,
                (rs, n) -> new TopProduct(rs.getString("product_sku"), rs.getString("product_name"),
                        rs.getInt("quantity"), rs.getBigDecimal("revenue"), rs.getBigDecimal("profit")),
                JdbcHelper.timestamp(from), JdbcHelper.timestamp(to), limit);
    }

    public List<DailySales> dailySales(LocalDateTime from, LocalDateTime to) {
        return jdbc.query("""
                SELECT CAST(issued_at AS DATE) AS sale_date, COUNT(*) AS invoices, SUM(total) AS total
                FROM invoices
                WHERE status = 'PAID' AND issued_at >= ? AND issued_at < ?
                GROUP BY CAST(issued_at AS DATE)
                ORDER BY sale_date
                """,
                (rs, n) -> new DailySales(rs.getDate("sale_date").toLocalDate(), rs.getInt("invoices"),
                        rs.getBigDecimal("total")),
                JdbcHelper.timestamp(from), JdbcHelper.timestamp(to));
    }
}
