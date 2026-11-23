package co.edu.ucc.stockflow.reporting.application;

import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.catalog.domain.ProductRepository;
import co.edu.ucc.stockflow.customers.domain.CustomerRepository;
import co.edu.ucc.stockflow.shared.util.DateRange;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Numbers for the dashboard and the reports section. */
@Service
public class ReportService {

    private final ReportQueries reports;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final Clock clock;

    public ReportService(ReportQueries reports, ProductRepository products, CustomerRepository customers,
                         Clock clock) {
        this.reports = reports;
        this.products = products;
        this.customers = customers;
        this.clock = clock;
    }

    public DashboardSummary dashboard() {
        DateRange month = DateRange.thisMonth(clock);
        DateRange today = DateRange.today(clock);
        List<Product> lowStock = products.findLowStock();
        return new DashboardSummary(
                reports.salesSummary(today.start(), today.end()),
                reports.salesSummary(month.start(), month.end()),
                products.countActive(),
                lowStock.size(),
                products.totalUnits(),
                products.totalStockValue(),
                customers.count(),
                lowStock.size() > 8 ? lowStock.subList(0, 8) : lowStock,
                reports.topProducts(month.start(), month.end(), 5),
                dailySales(DateRange.lastDays(7, clock)));
    }

    public SalesSummary salesSummary(DateRange range) {
        return reports.salesSummary(range.start(), range.end());
    }

    public List<TopProduct> topProducts(DateRange range, int limit) {
        return reports.topProducts(range.start(), range.end(), limit);
    }

    /** Units sold of each product in the range, by product id. */
    public Map<Long, Integer> unitsSoldByProduct(DateRange range) {
        return reports.unitsSoldByProduct(range.start(), range.end());
    }

    /** One entry per day of the range, including the days without sales (with 0). */
    public List<DailySales> dailySales(DateRange range) {
        Map<LocalDate, DailySales> byDay = new HashMap<>();
        for (DailySales d : reports.dailySales(range.start(), range.end())) byDay.put(d.date(), d);
        List<DailySales> all = new ArrayList<>();
        for (LocalDate d = range.start().toLocalDate(); d.isBefore(range.end().toLocalDate()); d = d.plusDays(1)) {
            all.add(byDay.getOrDefault(d, new DailySales(d, 0, BigDecimal.ZERO)));
        }
        return all;
    }
}
