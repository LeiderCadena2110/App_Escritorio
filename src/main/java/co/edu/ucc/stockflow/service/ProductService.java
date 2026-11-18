package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.exception.ErrorCode;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.model.ProductCreated;
import co.edu.ucc.stockflow.repository.ProductRepository;
import co.edu.ucc.stockflow.util.Formats;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final ApplicationEventPublisher events;

    public ProductService(ProductRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    public List<Product> search(String text, Long categoryId, boolean lowStockOnly, boolean includeInactive) {
        return repository.search(text, categoryId, lowStockOnly, includeInactive);
    }

    public List<Product> findActive() {
        return repository.findActive();
    }

    public Optional<Product> findById(long id) {
        return repository.findById(id);
    }

    public Optional<Product> findBySku(String sku) {
        return repository.findBySku(sku);
    }

    /** Product with that exact code: SKU first, then barcode (what a scanner types). */
    public Optional<Product> findByCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        Optional<Product> bySku = repository.findBySku(code);
        return bySku.isPresent() ? bySku : repository.findByBarcode(code);
    }

    /**
     * Creates a product. If it has units, it publishes {@link ProductCreated} and the inventory
     * registers them as the initial inventory, so the stock history starts with them.
     */
    @Transactional
    public Product create(Product product) {
        validate(product);
        int initialStock = product.getStock();
        if (initialStock < 0) throw new BusinessException(ErrorCode.PRODUCT_INITIAL_STOCK_NEGATIVE);
        product.setStock(0);
        Product saved = repository.insert(product);
        if (initialStock > 0) {
            events.publishEvent(new ProductCreated(saved.getId(), initialStock, saved.getCostPrice()));
            saved.setStock(initialStock);
        }
        return saved;
    }

    @Transactional
    public Product update(Product product) {
        if (product.getId() == null) throw new IllegalArgumentException("The product has no id");
        validate(product);
        repository.update(product);
        return product;
    }

    /**
     * Removes the product. Products that were sold or had any stock movement after being created
     * (a purchase, an adjustment...) are only deactivated, so invoices and the stock history stay complete.
     *
     * @return true if it was deleted, false if it was deactivated
     */
    @Transactional
    public boolean delete(Product product) {
        if (repository.countSales(product.getId()) > 0 || repository.countHistory(product.getId()) > 0) {
            repository.setActive(product.getId(), false);
            return false;
        }
        repository.delete(product.getId());
        return true;
    }

    @Transactional
    public void setActive(Product product, boolean active) {
        repository.setActive(product.getId(), active);
        product.setActive(active);
    }

    private void validate(Product p) {
        p.setSku(Formats.clean(p.getSku()));
        p.setBarcode(Formats.clean(p.getBarcode()));
        p.setName(Formats.clean(p.getName()));
        p.setDescription(Formats.clean(p.getDescription()));
        if (p.getSku() == null) throw new BusinessException(ErrorCode.PRODUCT_SKU_REQUIRED);
        if (p.getSku().length() > 50) throw new BusinessException(ErrorCode.PRODUCT_SKU_TOO_LONG, 50);
        if (p.getName() == null) throw new BusinessException(ErrorCode.PRODUCT_NAME_REQUIRED);
        if (p.getBarcode() != null && !p.getBarcode().matches("[0-9A-Za-z\\-]{4,50}")) {
            throw new BusinessException(ErrorCode.PRODUCT_BARCODE_INVALID);
        }
        if (p.getCostPrice().signum() < 0) throw new BusinessException(ErrorCode.PRODUCT_COST_NEGATIVE);
        if (p.getSalePrice().signum() <= 0) throw new BusinessException(ErrorCode.PRODUCT_PRICE_NOT_POSITIVE);
        if (p.getMinStock() < 0) throw new BusinessException(ErrorCode.PRODUCT_MIN_STOCK_NEGATIVE);
        if (p.getTaxRate().signum() < 0 || p.getTaxRate().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException(ErrorCode.PRODUCT_TAX_RATE_OUT_OF_RANGE);
        }
        if (p.getUnit() == null || p.getUnit().isBlank()) p.setUnit("UND");
        repository.findBySku(p.getSku())
                .filter(other -> !other.getId().equals(p.getId()))
                .ifPresent(other -> {
                    throw new BusinessException(ErrorCode.PRODUCT_SKU_DUPLICATED, p.getSku(),
                            other.getName());
                });
        if (p.getBarcode() != null) {
            repository.findByBarcode(p.getBarcode())
                    .filter(other -> !other.getId().equals(p.getId()))
                    .ifPresent(other -> {
                        throw new BusinessException(ErrorCode.PRODUCT_BARCODE_DUPLICATED,
                                p.getBarcode(), other.getSku(), other.getName());
                    });
        }
    }
}
