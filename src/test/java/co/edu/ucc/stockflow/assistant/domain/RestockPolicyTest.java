package co.edu.ucc.stockflow.assistant.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.catalog.domain.Product;
import org.junit.jupiter.api.Test;

class RestockPolicyTest {

    private final RestockPolicy policy = RestockPolicy.standard(); // 30 days, 7 to deliver, buy for 30

    private static Product product(int stock, int minStock) {
        Product p = new Product();
        p.setSku("P-1");
        p.setName("Tenis");
        p.setStock(stock);
        p.setMinStock(minStock);
        return p;
    }

    @Test
    void fastSellerThatRunsOutSoonIsUrgent() {
        // 60 sold in 30 days = 2 per day; 10 units last 5 days, less than the 7 of delivery
        RestockSuggestion s = policy.evaluate(product(10, 4), 60).orElseThrow();
        assertEquals(5, s.daysLeft());
        assertEquals(RestockPriority.URGENT, s.priority());
        assertEquals(2 * (7 + 30) - 10, s.suggestedQuantity()); // 74 - 10 = 64
    }

    @Test
    void stockThatLastsLessThanDeliveryPlusAWeekIsSoon() {
        // 1 per day; 10 units last 10 days (< 7 + 7)
        RestockSuggestion s = policy.evaluate(product(10, 2), 30).orElseThrow();
        assertEquals(RestockPriority.SOON, s.priority());
        assertEquals(37 - 10, s.suggestedQuantity());
    }

    @Test
    void enoughStockNeedsNothing() {
        assertTrue(policy.evaluate(product(40, 5), 30).isEmpty()); // lasts 40 days
    }

    @Test
    void productThatDidNotSellIsOnlyBoughtWhenBelowTheMinimum() {
        assertTrue(policy.evaluate(product(9, 4), 0).isEmpty());
        RestockSuggestion s = policy.evaluate(product(3, 4), 0).orElseThrow();
        assertNull(s.daysLeft());
        assertEquals(8 - 3, s.suggestedQuantity()); // up to twice the minimum
        assertEquals(RestockPriority.SOON, s.priority());
    }

    @Test
    void soldOutIsAlwaysUrgent() {
        assertEquals(RestockPriority.URGENT, policy.evaluate(product(0, 2), 0).orElseThrow().priority());
    }

    @Test
    void inactiveProductsAreNeverSuggested() {
        Product p = product(0, 5);
        p.setActive(false);
        assertTrue(policy.evaluate(p, 50).isEmpty());
    }
}
