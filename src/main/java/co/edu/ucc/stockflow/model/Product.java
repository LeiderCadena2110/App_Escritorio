package co.edu.ucc.stockflow.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/** Item that the business sells and keeps in stock. */
public class Product {

    private Long id;
    private String sku;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long supplierId;
    private String supplierName;
    private BigDecimal costPrice = BigDecimal.ZERO;
    private BigDecimal salePrice = BigDecimal.ZERO;
    private int stock;
    private int minStock;
    private String unit = "UND";
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Stock at or below the minimum: shown as a warning. */
    public boolean isLowStock() {
        return active && stock <= minStock;
    }

    /** Value of the units in stock at cost price. */
    public BigDecimal getStockValue() {
        return costPrice.multiply(BigDecimal.valueOf(stock));
    }

    /** Profit margin over the sale price, in percent (0 when the price is 0). */
    public BigDecimal getMarginPercent() {
        if (salePrice.signum() == 0) return BigDecimal.ZERO;
        return salePrice.subtract(costPrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(salePrice, 1, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice == null ? BigDecimal.ZERO : costPrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice == null ? BigDecimal.ZERO : salePrice; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Product p && id != null && id.equals(p.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return sku + " · " + name;
    }
}
