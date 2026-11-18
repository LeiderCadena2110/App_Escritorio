package co.edu.ucc.stockflow.sales.domain;

import co.edu.ucc.stockflow.catalog.domain.Product;
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
    /** VAT rate of the product when it was added (what the product says). */
    private BigDecimal productTaxRate = BigDecimal.ZERO;
    /** VAT rate the line actually pays, after the business tax regime (saved with the invoice). */
    private BigDecimal taxRate = BigDecimal.ZERO;

    public InvoiceItem() {
    }

    /** Copies the current SKU, name, cost and VAT of the product (the price stays as offered to the customer). */
    public void refreshFrom(Product product) {
        productSku = product.getSku();
        productName = product.getName();
        unitCost = product.getCostPrice();
        productTaxRate = product.getTaxRate();
    }

    /** Copy of the line (used to hand a draft to the service without sharing objects with the screen). */
    public InvoiceItem copy() {
        InvoiceItem c = new InvoiceItem();
        c.productId = productId;
        c.productSku = productSku;
        c.productName = productName;
        c.unitPrice = unitPrice;
        c.unitCost = unitCost;
        c.productTaxRate = productTaxRate;
        c.taxRate = taxRate;
        c.setQuantity(quantity);
        return c;
    }

    public static InvoiceItem of(Product product, int quantity) {
        InvoiceItem item = new InvoiceItem();
        item.setProductId(product.getId());
        item.setProductSku(product.getSku());
        item.setProductName(product.getName());
        item.setUnitPrice(product.getSalePrice());
        item.setUnitCost(product.getCostPrice());
        item.setProductTaxRate(product.getTaxRate());
        item.setTaxRate(product.getTaxRate());
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
    public BigDecimal getProductTaxRate() { return productTaxRate; }
    public void setProductTaxRate(BigDecimal rate) { this.productTaxRate = rate == null ? BigDecimal.ZERO : rate; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal rate) { this.taxRate = rate == null ? BigDecimal.ZERO : rate; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
}
