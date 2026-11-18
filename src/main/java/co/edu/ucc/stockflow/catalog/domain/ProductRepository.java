package co.edu.ucc.stockflow.catalog.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Where products are stored and searched.
 *
 * <p>Port: the application layer depends on this interface and JdbcProductRepository
 * (infrastructure) implements it with SQL, so tests can use an in-memory version instead.</p>
 */
public interface ProductRepository {

    /**
     * Products that match the text (SKU, name or exact barcode), the category and the stock filter.
     * Any argument may be null/false to skip that filter.
     */
    List<Product> search(String text, Long categoryId, boolean lowStockOnly, boolean includeInactive);

    List<Product> findActive();

    List<Product> findLowStock();

    Optional<Product> findById(long id);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    Product insert(Product p);

    /** Updates the product data. The stock is NOT changed here: it only changes through stock movements. */
    void update(Product p);

    void updateStock(long id, int newStock);

    void updateCostPrice(long id, BigDecimal costPrice);

    void setActive(long id, boolean active);

    /**
     * Deletes a product that never moved: only its "initial inventory" entry is removed with it.
     * Products with any other movement must be deactivated instead (see ProductService.delete).
     */
    void delete(long id);

    /** Movements other than the initial inventory (purchases, sales, adjustments...). */
    int countHistory(long id);

    int countSales(long id);

    int countActive();

    int countLowStock();

    /** Value of all the units in stock at cost price. */
    BigDecimal totalStockValue();

    int totalUnits();
}
