package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.repository.ProductRepository;
import co.edu.ucc.stockflow.util.Formats;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final StockService stockService;

    public ProductService(ProductRepository repository, StockService stockService) {
        this.repository = repository;
        this.stockService = stockService;
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

    /**
     * Creates a product. If it has units, they are registered as the initial inventory so the stock
     * history starts with them.
     */
    @Transactional
    public Product create(Product product) {
        validate(product);
        int initialStock = product.getStock();
        if (initialStock < 0) throw new BusinessException("El stock inicial no puede ser negativo.");
        product.setStock(0);
        Product saved = repository.insert(product);
        if (initialStock > 0) {
            stockService.register(saved.getId(), MovementType.INITIAL, initialStock, saved.getCostPrice(),
                    null, "Stock al crear el producto");
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
     * Removes the product. Products that were already sold are only deactivated so the invoices keep
     * their history.
     *
     * @return true if it was deleted, false if it was deactivated
     */
    @Transactional
    public boolean delete(Product product) {
        if (repository.countSales(product.getId()) > 0) {
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
        p.setName(Formats.clean(p.getName()));
        p.setDescription(Formats.clean(p.getDescription()));
        if (p.getSku() == null) throw new BusinessException("Escriba el código (SKU) del producto.");
        if (p.getSku().length() > 50) throw new BusinessException("El código (SKU) puede tener máximo 50 caracteres.");
        if (p.getName() == null) throw new BusinessException("Escriba el nombre del producto.");
        if (p.getCostPrice().signum() < 0) throw new BusinessException("El precio de costo no puede ser negativo.");
        if (p.getSalePrice().signum() <= 0) throw new BusinessException("El precio de venta debe ser mayor que 0.");
        if (p.getMinStock() < 0) throw new BusinessException("El stock mínimo no puede ser negativo.");
        if (p.getUnit() == null || p.getUnit().isBlank()) p.setUnit("UND");
        repository.findBySku(p.getSku())
                .filter(other -> !other.getId().equals(p.getId()))
                .ifPresent(other -> {
                    throw new BusinessException("Ya existe un producto con el código " + p.getSku()
                            + " (" + other.getName() + ").");
                });
    }
}
