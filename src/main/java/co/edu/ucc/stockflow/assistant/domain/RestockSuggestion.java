package co.edu.ucc.stockflow.assistant.domain;

import co.edu.ucc.stockflow.catalog.domain.Product;

/**
 * What the restock plan says about one product.
 *
 * @param product           the product (with its current stock and minimum)
 * @param unitsSold         units sold in the period analysed
 * @param dailyRate         average units sold per day
 * @param daysLeft          days the current stock lasts at that rate, or null if it did not sell
 * @param suggestedQuantity units to buy
 * @param priority          how soon
 */
public record RestockSuggestion(Product product, int unitsSold, double dailyRate, Integer daysLeft,
                                int suggestedQuantity, RestockPriority priority) {
}
