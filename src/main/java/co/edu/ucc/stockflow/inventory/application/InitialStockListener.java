package co.edu.ucc.stockflow.inventory.application;

import co.edu.ucc.stockflow.catalog.domain.ProductCreated;
import co.edu.ucc.stockflow.inventory.domain.MovementType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Registers the units a product was created with as its first stock movement. Spring calls it in
 * the same thread and transaction as the creation, so if this fails the product is not saved either.
 */
@Component
public class InitialStockListener {

    private final StockService stock;

    public InitialStockListener(StockService stock) {
        this.stock = stock;
    }

    @EventListener
    public void on(ProductCreated event) {
        if (event.initialStock() <= 0) return;
        stock.register(event.productId(), MovementType.INITIAL, event.initialStock(), event.unitCost(),
                null, "Stock al crear el producto");
    }
}
