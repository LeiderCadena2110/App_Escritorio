package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.exception.InsufficientStockException;
import co.edu.ucc.stockflow.exception.NotFoundException;
import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.model.StockMovement;
import co.edu.ucc.stockflow.repository.ProductRepository;
import co.edu.ucc.stockflow.repository.StockMovementRepository;
import co.edu.ucc.stockflow.util.DateRange;
import co.edu.ucc.stockflow.util.Formats;
import co.edu.ucc.stockflow.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only place where the stock of a product changes. Every change leaves a movement in the
 * history (kardex) with the resulting stock.
 */
@Service
public class StockService {

    private final ProductRepository products;
    private final StockMovementRepository movements;

    public StockService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    /**
     * Registers a stock movement.
     *
     * @param quantity units, always positive except for {@link MovementType#ADJUSTMENT}, where a negative
     *                 number removes units
     * @param unitCost purchase cost per unit (only for purchases; may be null)
     */
    @Transactional
    public StockMovement register(long productId, MovementType type, int quantity, BigDecimal unitCost,
                                  String reference, String notes) {
        Product product = products.findById(productId).orElseThrow(() -> new NotFoundException("el producto", productId));
        int change = signedQuantity(type, quantity);
        int newStock = product.getStock() + change;
        if (newStock < 0) {
            throw new InsufficientStockException(product.getSku(), product.getName(), product.getStock(), -change);
        }
        if (type == MovementType.PURCHASE && unitCost != null && unitCost.signum() > 0) {
            products.updateCostPrice(productId, averageCost(product, quantity, unitCost));
        }
        products.updateStock(productId, newStock);

        StockMovement m = new StockMovement();
        m.setProductId(productId);
        m.setProductSku(product.getSku());
        m.setProductName(product.getName());
        m.setType(type);
        m.setQuantity(change);
        m.setStockAfter(newStock);
        m.setUnitCost(unitCost != null ? Money.round(unitCost) : product.getCostPrice());
        m.setReference(Formats.clean(reference));
        m.setNotes(Formats.clean(notes));
        return movements.insert(m);
    }

    /** Applies the sign of the movement type and checks the quantity. */
    static int signedQuantity(MovementType type, int quantity) {
        if (type.sign() == 0) {
            if (quantity == 0) throw new BusinessException("La cantidad del ajuste no puede ser 0.");
            return quantity;
        }
        if (quantity <= 0) throw new BusinessException("La cantidad debe ser mayor que 0.");
        return quantity * type.sign();
    }

    /**
     * Weighted average cost after a purchase:
     * (current stock × current cost + purchased units × purchase cost) / (current stock + purchased units).
     */
    static BigDecimal averageCost(Product product, int purchased, BigDecimal purchaseCost) {
        int current = Math.max(product.getStock(), 0);
        BigDecimal totalUnits = BigDecimal.valueOf((long) current + purchased);
        if (totalUnits.signum() == 0) return Money.round(purchaseCost);
        BigDecimal value = product.getCostPrice().multiply(BigDecimal.valueOf(current))
                .add(purchaseCost.multiply(BigDecimal.valueOf(purchased)));
        return value.divide(totalUnits, Money.SCALE, RoundingMode.HALF_UP);
    }

    public List<StockMovement> history(long productId) {
        return movements.findByProduct(productId);
    }

    public List<StockMovement> search(DateRange range, Long productId, MovementType type) {
        return movements.search(range == null ? null : range.start(), range == null ? null : range.end(),
                productId, type);
    }
}
