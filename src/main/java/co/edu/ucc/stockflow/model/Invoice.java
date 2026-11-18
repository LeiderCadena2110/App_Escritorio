package co.edu.ucc.stockflow.model;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.exception.ErrorCode;
import co.edu.ucc.stockflow.model.tax.TaxLine;
import co.edu.ucc.stockflow.model.tax.TaxPolicy;
import co.edu.ucc.stockflow.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Sales invoice: an aggregate that owns its lines and keeps its totals consistent.
 *
 * <p>Lines are added and changed through {@link #addLine}, {@link #changeQuantity} and
 * {@link #removeLine}, which apply the stock rule of the product. {@link #calculate(TaxPolicy)}
 * computes subtotal, discount, VAT broken down by rate, and total in one place, for the point of
 * sale preview and for the invoice that is saved.</p>
 */
public class Invoice {

    private Long id;
    private String number;
    private Long customerId;
    private String customerName;
    private String customerDocument;
    private LocalDateTime issuedAt;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private BigDecimal taxRate = BigDecimal.ZERO;
    private BigDecimal tax = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    private BigDecimal amountPaid = BigDecimal.ZERO;
    private InvoiceStatus status = InvoiceStatus.PAID;
    private String notes;
    private LocalDateTime cancelledAt;
    private final List<InvoiceItem> items = new ArrayList<>();

    // ------------------------------------------------------------------ lines

    /**
     * Adds units of a product, or more units to its line if it is already in the invoice.
     *
     * @return the line that was added or changed
     * @throws co.edu.ucc.stockflow.exception.InsufficientStockException if the product has not enough stock
     */
    public InvoiceItem addLine(Product product, int units) {
        if (units <= 0) throw new BusinessException(ErrorCode.INVOICE_QUANTITY_NOT_POSITIVE);
        Optional<InvoiceItem> existing = lineOf(product.getId());
        int wanted = existing.map(i -> i.getQuantity() + units).orElse(units);
        product.checkCanTakeOut(wanted);
        if (existing.isPresent()) {
            existing.get().setQuantity(wanted);
            return existing.get();
        }
        InvoiceItem line = InvoiceItem.of(product, units);
        items.add(line);
        return line;
    }

    /**
     * Changes the units of a line; 0 or less removes it.
     *
     * @param product latest data of the product to check its stock (null skips the check)
     */
    public void changeQuantity(InvoiceItem line, int units, Product product) {
        if (!items.contains(line)) throw new IllegalArgumentException("The line is not in this invoice");
        if (units <= 0) {
            items.remove(line);
            return;
        }
        if (product != null) product.checkCanTakeOut(units);
        line.setQuantity(units);
    }

    public void removeLine(InvoiceItem line) {
        items.remove(line);
    }

    public Optional<InvoiceItem> lineOf(Long productId) {
        return items.stream().filter(i -> i.getProductId().equals(productId)).findFirst();
    }

    /** Total units of each product (a product appears once even if it came in several lines). */
    public Map<Long, Integer> unitsByProduct() {
        Map<Long, Integer> units = new LinkedHashMap<>();
        for (InvoiceItem i : items) units.merge(i.getProductId(), i.getQuantity(), Integer::sum);
        return units;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    // ------------------------------------------------------------------ totals

    /** Gross value of the lines (price × units), before discount and VAT. */
    public BigDecimal grossSubtotal() {
        return Money.round(items.stream().map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Checks the discount the user asked for: it cannot be negative or bigger than the subtotal.
     * (The preview of the point of sale is lenient and caps it; issuing an invoice is strict.)
     */
    public void validateDiscount() {
        BigDecimal d = discount == null ? BigDecimal.ZERO : discount;
        if (d.signum() < 0) throw new BusinessException(ErrorCode.INVOICE_DISCOUNT_NEGATIVE);
        if (d.compareTo(grossSubtotal()) > 0) throw new BusinessException(ErrorCode.INVOICE_DISCOUNT_ABOVE_SUBTOTAL);
    }

    /**
     * Computes every total of the invoice:
     * <ol>
     *   <li>each line pays the VAT rate the {@code policy} gives it;</li>
     *   <li>subtotal = sum of the lines; the discount is capped between 0 and the subtotal;</li>
     *   <li>VAT = sum of {@link #getTaxBreakdown()} (discount spread over the rates);</li>
     *   <li>total = subtotal − discount + VAT.</li>
     * </ol>
     */
    public void calculate(TaxPolicy policy) {
        for (InvoiceItem line : items) {
            line.recalculate();
            line.setTaxRate(policy.rateFor(line.getProductTaxRate()));
        }
        subtotal = grossSubtotal();
        BigDecimal d = Money.round(discount);
        if (d.signum() < 0) d = BigDecimal.ZERO;
        if (d.compareTo(subtotal) > 0) d = subtotal;
        discount = d;
        tax = getTaxBreakdown().stream().map(TaxLine::tax).reduce(BigDecimal.ZERO, BigDecimal::add);
        total = subtotal.subtract(discount).add(tax);
        List<BigDecimal> rates = items.stream().map(InvoiceItem::getTaxRate).distinct().toList();
        taxRate = rates.size() == 1 ? rates.get(0) : null;
    }

    /**
     * VAT grouped by rate, highest rate first. The invoice discount is spread over the rates in
     * proportion to their value, in whole pesos (the biggest group absorbs the rounding), and each
     * group pays base × rate / 100 rounded to the peso, as Colombian invoices do.
     */
    public List<TaxLine> getTaxBreakdown() {
        Map<BigDecimal, BigDecimal> grossByRate = new TreeMap<>(Comparator.reverseOrder());
        for (InvoiceItem i : items) {
            BigDecimal line = i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            grossByRate.merge(i.getTaxRate().stripTrailingZeros(), line, BigDecimal::add);
        }
        if (grossByRate.isEmpty()) return List.of();
        BigDecimal gross = grossByRate.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal d = Money.round(discount);
        if (d.signum() < 0) d = BigDecimal.ZERO;
        if (d.compareTo(gross) > 0) d = Money.round(gross);

        BigDecimal biggestRate = grossByRate.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        Map<BigDecimal, BigDecimal> discountByRate = new TreeMap<>(Comparator.reverseOrder());
        BigDecimal spread = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, BigDecimal> e : grossByRate.entrySet()) {
            if (e.getKey().equals(biggestRate) || gross.signum() == 0) continue;
            BigDecimal share = d.multiply(e.getValue()).divide(gross, 0, RoundingMode.HALF_UP).setScale(Money.SCALE);
            discountByRate.put(e.getKey(), share);
            spread = spread.add(share);
        }
        discountByRate.put(biggestRate, d.subtract(spread));

        List<TaxLine> lines = new ArrayList<>();
        for (Map.Entry<BigDecimal, BigDecimal> e : grossByRate.entrySet()) {
            BigDecimal base = Money.round(e.getValue()).subtract(discountByRate.get(e.getKey()));
            BigDecimal tax = Money.percentOf(base, e.getKey()).setScale(0, RoundingMode.HALF_UP).setScale(Money.SCALE);
            lines.add(new TaxLine(e.getKey(), base, tax));
        }
        return lines;
    }

    /** The rate of the invoice when every line pays the same one (empty when rates are mixed). */
    public Optional<BigDecimal> singleTaxRate() {
        List<BigDecimal> rates = items.stream().map(i -> i.getTaxRate().stripTrailingZeros()).distinct().toList();
        return rates.size() == 1 ? Optional.of(rates.get(0)) : Optional.empty();
    }

    /** Copy of the draft with its own lines (the screen keeps editing its draft while the copy is saved). */
    public Invoice copy() {
        Invoice c = new Invoice();
        for (InvoiceItem i : items) c.items.add(i.copy());
        c.customerId = customerId;
        c.paymentMethod = paymentMethod;
        c.discount = discount;
        c.amountPaid = amountPaid;
        c.notes = notes;
        return c;
    }

    // ------------------------------------------------------------------ payment

    /** Change to give back when paying in cash (never negative). */
    public BigDecimal getChange() {
        BigDecimal change = amountPaid.subtract(total);
        return change.signum() < 0 ? BigDecimal.ZERO : change;
    }

    public int getItemCount() {
        return items.stream().mapToInt(InvoiceItem::getQuantity).sum();
    }

    public boolean isCancelled() {
        return status == InvoiceStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerDocument() { return customerDocument; }
    public void setCustomerDocument(String customerDocument) { this.customerDocument = customerDocument; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    /** The rate when all lines pay the same one; null when rates are mixed (see getTaxBreakdown). */
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    /** The lines, read-only: they change through addLine, changeQuantity and removeLine. */
    public List<InvoiceItem> getItems() { return Collections.unmodifiableList(items); }

    /** Puts back the lines of an invoice read from the database (used by the repository). */
    public void restoreLines(List<InvoiceItem> saved) {
        items.clear();
        items.addAll(saved);
    }
}
