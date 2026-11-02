package co.edu.ucc.stockflow.dto;

import co.edu.ucc.stockflow.model.Product;
import java.math.BigDecimal;
import java.util.List;

/** Everything the dashboard shows, loaded in one call. */
public record DashboardSummary(SalesSummary today, SalesSummary month, int activeProducts, int lowStockCount,
                               int totalUnits, BigDecimal stockValue, int customers, List<Product> lowStock,
                               List<TopProduct> topProducts, List<DailySales> lastDays) {
}
