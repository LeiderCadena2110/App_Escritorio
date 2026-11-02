package co.edu.ucc.stockflow.model;

import java.math.BigDecimal;

/**
 * One line of an invoice. SKU, name, price and cost are copied from the product at the
 * moment of the sale so the invoice does not change if the product is edited later.
 */
public class InvoiceItem {

    private Long id;
    private Long invoiceId;
    private Long productId;
    private String productSku;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice = BigDecimal.ZERO;
    private BigDecimal unitCost = BigDecimal.ZERO;
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public InvoiceItem() {
    }

    public static InvoiceItem of(Product product, int quantity) {
        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setProductSku(product.getSku());
        item.setProductName(product.getName());
        item.setUnitPrice(product.getSalePrice());
        item.setUnitCost(product.getCostPrice());
        item.setQuantity(quantity);
        return item;
    }

    /** Recalculates the line total after a quantity or price change. */
    public void recalculate() {
        lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal getProfit() {
        return unitPrice.subtract(unitCost).multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductSku() { return productSku; }
    public void setProductSku(String productSku) { this.productSku = productSku; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; recalculate(); }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; recalculate(); }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
