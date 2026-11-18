package co.edu.ucc.stockflow.model;

import java.math.BigDecimal;

/**
 * Domain event: a product was created with some units in stock. The catalog publishes it and the
 * inventory registers the "initial inventory" movement, so the catalog does not depend on the inventory.
 */
public record ProductCreated(long productId, int initialStock, BigDecimal unitCost) {
}
