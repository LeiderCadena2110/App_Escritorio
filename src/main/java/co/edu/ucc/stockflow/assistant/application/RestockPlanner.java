package co.edu.ucc.stockflow.assistant.application;

import co.edu.ucc.stockflow.assistant.domain.RestockPolicy;
import co.edu.ucc.stockflow.assistant.domain.RestockSuggestion;
import co.edu.ucc.stockflow.catalog.application.ProductService;
import co.edu.ucc.stockflow.reporting.application.ReportService;
import co.edu.ucc.stockflow.shared.util.DateRange;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Builds the restock plan: which active products to buy and how many, most urgent first. */
@Service
public class RestockPlanner {

    private static final Comparator<RestockSuggestion> MOST_URGENT_FIRST = Comparator
            .comparing(RestockSuggestion::priority)
            .thenComparing(RestockSuggestion::daysLeft, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(s -> s.product().getName(), String.CASE_INSENSITIVE_ORDER);

    private final ProductService products;
    private final ReportService reports;
    private final Clock clock;

    public RestockPlanner(ProductService products, ReportService reports, Clock clock) {
        this.products = products;
        this.reports = reports;
        this.clock = clock;
    }

    public List<RestockSuggestion> plan(RestockPolicy policy) {
        Map<Long, Integer> sold = reports.unitsSoldByProduct(DateRange.lastDays(policy.periodDays(), clock));
        return products.findActive().stream()
                .map(p -> policy.evaluate(p, sold.getOrDefault(p.getId(), 0)))
                .flatMap(Optional::stream)
                .sorted(MOST_URGENT_FIRST)
                .toList();
    }
}
