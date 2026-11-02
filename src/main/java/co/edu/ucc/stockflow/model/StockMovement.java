package co.edu.ucc.stockflow.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Entry in the stock history (kardex) of a product. Quantity is positive for entries, negative for exits. */
public class StockMovement {

    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private MovementType type;
    private int quantity;
    private int stockAfter;
    private BigDecimal unitCost;
    private String reference;
    private String notes;
    private LocalDateTime createdAt;

    public boolean isEntry() {
        return quantity > 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public MovementType getType() { return type; }
    public void setType(MovementType type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getStockAfter() { return stockAfter; }
    public void setStockAfter(int stockAfter) { this.stockAfter = stockAfter; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
