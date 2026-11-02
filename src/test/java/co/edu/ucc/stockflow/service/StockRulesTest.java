package co.edu.ucc.stockflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class StockRulesTest {

    @Test
    void entriesAddAndExitsRemove() {
        assertEquals(5, StockService.signedQuantity(MovementType.PURCHASE, 5));
        assertEquals(-5, StockService.signedQuantity(MovementType.SALE, 5));
        assertEquals(-2, StockService.signedQuantity(MovementType.DAMAGE, 2));
        assertEquals(3, StockService.signedQuantity(MovementType.SALE_CANCELLATION, 3));
    }

    @Test
    void adjustmentsKeepTheirSign() {
        assertEquals(-4, StockService.signedQuantity(MovementType.ADJUSTMENT, -4));
        assertEquals(4, StockService.signedQuantity(MovementType.ADJUSTMENT, 4));
        assertThrows(BusinessException.class, () -> StockService.signedQuantity(MovementType.ADJUSTMENT, 0));
    }

    @Test
    void quantitiesMustBePositive() {
        assertThrows(BusinessException.class, () -> StockService.signedQuantity(MovementType.PURCHASE, 0));
        assertThrows(BusinessException.class, () -> StockService.signedQuantity(MovementType.SALE, -1));
    }

    @Test
    void purchaseUpdatesTheWeightedAverageCost() {
        Product p = new Product();
        p.setStock(16);
        p.setCostPrice(new BigDecimal("145000"));
        // (16 × 145.000 + 10 × 150.000) / 26 = 146.923,08
        assertEquals(new BigDecimal("146923.08"), StockService.averageCost(p, 10, new BigDecimal("150000")));
    }

    @Test
    void firstPurchaseUsesThePurchaseCost() {
        Product p = new Product();
        p.setStock(0);
        p.setCostPrice(BigDecimal.ZERO);
        assertEquals(new BigDecimal("80000.00"), StockService.averageCost(p, 4, new BigDecimal("80000")));
    }

    @Test
    void lowStockAndMargin() {
        Product p = new Product();
        p.setStock(3);
        p.setMinStock(3);
        p.setCostPrice(new BigDecimal("60000"));
        p.setSalePrice(new BigDecimal("100000"));
        assertTrue(p.isLowStock());
        assertEquals(new BigDecimal("40.0"), p.getMarginPercent());
        assertEquals(new BigDecimal("180000"), p.getStockValue());
        p.setStock(4);
        assertFalse(p.isLowStock());
    }

    @Test
    void onlySomeMovementTypesAreManual() {
        assertTrue(MovementType.PURCHASE.isManual());
        assertTrue(MovementType.ADJUSTMENT.isManual());
        assertFalse(MovementType.SALE.isManual());
        assertFalse(MovementType.SALE_CANCELLATION.isManual());
    }
}
