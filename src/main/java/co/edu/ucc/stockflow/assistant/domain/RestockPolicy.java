package co.edu.ucc.stockflow.assistant.domain;

import co.edu.ucc.stockflow.catalog.domain.Product;
import java.util.Optional;

/**
 * Decides whether a product must be bought again and how many units, from its stock, its minimum
 * and how fast it sold. Plain arithmetic, so every number can be explained and tested; the AI only
 * writes the summary, it never makes up quantities.
 *
 * <ul>
 *   <li>daily rate = units sold in the period / days of the period;</li>
 *   <li>days left = stock / daily rate;</li>
 *   <li>it needs restocking when the stock is at or below the minimum, or lasts less than
 *       delivery time + one week of margin;</li>
 *   <li>buy enough to cover delivery time + the coverage period, and never less than twice the minimum.</li>
 * </ul>
 *
 * @param periodDays    days of sales history analysed (for example 30)
 * @param leadTimeDays  days the supplier usually takes to deliver
 * @param coverageDays  days the purchase should last after it arrives
 */
public record RestockPolicy(int periodDays, int leadTimeDays, int coverageDays) {

    /** One week of margin over the delivery time before raising the alarm. */
    private static final int SAFETY_DAYS = 7;

    public static RestockPolicy standard() {
        return new RestockPolicy(30, 7, 30);
    }

    public RestockPolicy {
        if (periodDays <= 0 || leadTimeDays < 0 || coverageDays <= 0) {
            throw new IllegalArgumentException("Restock days must be positive");
        }
    }

    /** The suggestion for the product, or empty if it does not need to be bought now. */
    public Optional<RestockSuggestion> evaluate(Product product, int unitsSold) {
        if (!product.isActive()) return Optional.empty();
        int stock = Math.max(product.getStock(), 0);
        double rate = Math.max(unitsSold, 0) / (double) periodDays;
        Integer daysLeft = rate > 0 ? (int) Math.floor(stock / rate) : null;

        boolean belowMinimum = stock <= product.getMinStock();
        boolean runsOutSoon = daysLeft != null && daysLeft < leadTimeDays + SAFETY_DAYS;
        if (!belowMinimum && !runsOutSoon) return Optional.empty();

        int byRate = (int) Math.ceil(rate * (leadTimeDays + coverageDays));
        int target = Math.max(byRate, product.getMinStock() * 2);
        int suggested = target - stock;
        if (suggested <= 0) return Optional.empty();

        boolean urgent = stock == 0 || (daysLeft != null && daysLeft < leadTimeDays);
        return Optional.of(new RestockSuggestion(product, Math.max(unitsSold, 0), rate, daysLeft, suggested,
                urgent ? RestockPriority.URGENT : RestockPriority.SOON));
    }
}
