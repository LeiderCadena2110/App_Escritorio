package co.edu.ucc.stockflow.assistant.domain;

import co.edu.ucc.stockflow.catalog.domain.Product;
import java.math.BigDecimal;
import java.util.List;

/**
 * A purchase prepared from a supplier's invoice, waiting for the user to review it. Nothing is
 * saved until the user confirms: the AI proposes, the person decides.
 *
 * @param invoice what was read
 * @param lines   one per invoice line, with the product it seems to be
 */
public record PurchaseDraft(SupplierInvoice invoice, List<Line> lines) {

    public PurchaseDraft {
        lines = List.copyOf(lines);
    }

    /** Lines that will become stock entries. */
    public List<Line> included() {
        return lines.stream().filter(Line::willRegister).toList();
    }

    /** Units × cost of the included lines (before VAT). */
    public BigDecimal includedCost() {
        return included().stream().map(l -> l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** One line of the draft. Product, quantity and cost start with what was read and can be corrected. */
    public static final class Line {

        private final SupplierInvoice.Line source;
        private final MatchQuality quality;
        private Product product;
        private int quantity;
        private BigDecimal unitCost;
        private boolean include;

        public Line(SupplierInvoice.Line source, ProductMatcher.Match match) {
            this.source = source;
            this.quality = match.quality();
            this.product = match.product();
            this.quantity = source.quantity();
            this.unitCost = source.unitCost() == null ? BigDecimal.ZERO : source.unitCost();
            this.include = match.product() != null;
        }

        public boolean willRegister() {
            return include && product != null && quantity > 0;
        }

        public SupplierInvoice.Line getSource() { return source; }
        public MatchQuality getQuality() { return quality; }
        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost == null ? BigDecimal.ZERO : unitCost; }
        public boolean isInclude() { return include; }
        public void setInclude(boolean include) { this.include = include; }
    }
}
