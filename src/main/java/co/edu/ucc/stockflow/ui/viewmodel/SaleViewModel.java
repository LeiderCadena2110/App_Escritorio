package co.edu.ucc.stockflow.ui.viewmodel;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.service.InvoiceService;
import co.edu.ucc.stockflow.util.Formats;
import co.edu.ucc.stockflow.util.Money;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * State and rules of the sale being built in the point of sale (MVVM view model).
 *
 * <p>It knows nothing about the screen: the controller binds its fields and labels to these
 * properties, and the totals are recalculated automatically whenever something changes. That
 * makes the cart logic testable with plain JUnit.</p>
 */
public class SaleViewModel {

    private final ObservableList<InvoiceItem> items = FXCollections.observableArrayList();
    private final ObjectProperty<Customer> customer = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentMethod> paymentMethod = new SimpleObjectProperty<>(PaymentMethod.CASH);
    private final ObjectProperty<BigDecimal> discount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> received = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final StringProperty notes = new SimpleStringProperty("");
    private final ObjectProperty<BigDecimal> taxRate = new SimpleObjectProperty<>(BigDecimal.ZERO);

    private final ReadOnlyObjectWrapper<BigDecimal> subtotal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> appliedDiscount = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> tax = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> total = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> change = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyIntegerWrapper units = new ReadOnlyIntegerWrapper();
    private final ReadOnlyBooleanWrapper cash = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyStringWrapper warning = new ReadOnlyStringWrapper("");

    /** Latest data of each product in the catalog, to check the stock. */
    private final Map<Long, Product> catalog = new HashMap<>();
    /** Message of the last action that failed (shown until something else changes). */
    private String notice = "";

    public SaleViewModel() {
        Observable[] inputs = {items, customer, paymentMethod, discount, received, taxRate};
        for (Observable o : inputs) o.addListener(x -> {
            notice = "";
            recalculate();
        });
        recalculate();
    }

    // ------------------------------------------------------------------ catalog and cart

    /** Updates the stock known for each product (call it after reloading the catalog). */
    public void updateCatalog(Collection<Product> products) {
        catalog.clear();
        for (Product p : products) catalog.put(p.getId(), p);
    }

    /**
     * Adds one unit of the product, or one more if it is already in the sale.
     *
     * @return the line that was added or changed
     * @throws BusinessException if there is not enough stock
     */
    public InvoiceItem add(Product product) {
        Product p = catalog.getOrDefault(product.getId(), product);
        Optional<InvoiceItem> existing = find(p.getId());
        int wanted = existing.map(i -> i.getQuantity() + 1).orElse(1);
        checkStock(p.getId(), p.getName(), wanted);
        if (existing.isPresent()) {
            InvoiceItem item = existing.get();
            item.setQuantity(wanted);
            touch(item);
            return item;
        }
        InvoiceItem item = InvoiceItem.of(p, 1);
        items.add(item);
        return item;
    }

    /** Changes the quantity of a line; 0 or less removes it. */
    public void setQuantity(InvoiceItem item, int quantity) {
        if (quantity <= 0) {
            remove(item);
            return;
        }
        checkStock(item.getProductId(), item.getProductName(), quantity);
        item.setQuantity(quantity);
        touch(item);
    }

    public void increase(InvoiceItem item) {
        setQuantity(item, item.getQuantity() + 1);
    }

    public void decrease(InvoiceItem item) {
        setQuantity(item, item.getQuantity() - 1);
    }

    public void remove(InvoiceItem item) {
        items.remove(item);
    }

    /** Empties the sale and goes back to cash payment, without discount or notes. */
    public void clear(Customer defaultCustomer) {
        items.clear();
        discount.set(BigDecimal.ZERO);
        received.set(BigDecimal.ZERO);
        notes.set("");
        paymentMethod.set(PaymentMethod.CASH);
        customer.set(defaultCustomer);
        notice = "";
        recalculate();
    }

    /** Shows a message in the warning line until the next change (for example "Producto no encontrado"). */
    public void showNotice(String message) {
        notice = message == null ? "" : message;
        recalculate();
    }

    /** The invoice ready to be issued, with copies of the lines. */
    public Invoice toInvoice() {
        Invoice inv = new Invoice();
        for (InvoiceItem item : items) {
            InvoiceItem copy = new InvoiceItem();
            copy.setProductId(item.getProductId());
            copy.setProductSku(item.getProductSku());
            copy.setProductName(item.getProductName());
            copy.setUnitCost(item.getUnitCost());
            copy.setUnitPrice(item.getUnitPrice());
            copy.setQuantity(item.getQuantity());
            inv.getItems().add(copy);
        }
        inv.setCustomerId(customer.get() == null ? null : customer.get().getId());
        inv.setPaymentMethod(paymentMethod.get());
        inv.setDiscount(discount.get());
        inv.setAmountPaid(paymentMethod.get() == PaymentMethod.CASH ? received.get() : BigDecimal.ZERO);
        inv.setNotes(Formats.clean(notes.get()));
        return inv;
    }

    private Optional<InvoiceItem> find(Long productId) {
        return items.stream().filter(i -> i.getProductId().equals(productId)).findFirst();
    }

    private void checkStock(Long productId, String name, int wanted) {
        Product p = catalog.get(productId);
        if (p == null || wanted <= p.getStock()) return;
        throw new BusinessException(p.getStock() == 0 ? name + " está agotado."
                : "Solo hay " + p.getStock() + " " + p.getUnit() + " de " + name + ".");
    }

    /** Tells the list (and the table) that a line changed. */
    private void touch(InvoiceItem item) {
        int index = items.indexOf(item);
        if (index >= 0) items.set(index, item);
    }

    // ------------------------------------------------------------------ totals

    private void recalculate() {
        Invoice preview = toInvoice();
        BigDecimal rate = taxRate.get() == null ? BigDecimal.ZERO : taxRate.get();
        InvoiceService.calculateTotals(preview, rate);
        subtotal.set(preview.getSubtotal());
        appliedDiscount.set(preview.getDiscount());
        tax.set(preview.getTax());
        total.set(preview.getTotal());
        units.set(items.stream().mapToInt(InvoiceItem::getQuantity).sum());
        empty.set(items.isEmpty());
        cash.set(paymentMethod.get() == PaymentMethod.CASH);

        BigDecimal paid = received.get() == null ? BigDecimal.ZERO : received.get();
        BigDecimal difference = paid.subtract(preview.getTotal());
        change.set(cash.get() && paid.signum() > 0 && difference.signum() > 0 ? difference : BigDecimal.ZERO);

        String w = "";
        BigDecimal requested = discount.get() == null ? BigDecimal.ZERO : discount.get();
        if (!items.isEmpty() && requested.compareTo(preview.getSubtotal()) > 0) {
            w = "El descuento es mayor que el subtotal.";
        } else if (cash.get() && paid.signum() > 0 && difference.signum() < 0) {
            w = "Faltan " + Money.format(difference.negate()) + " para completar el pago.";
        }
        warning.set(notice.isBlank() ? w : notice);
    }

    // ------------------------------------------------------------------ properties

    public ObservableList<InvoiceItem> getItems() { return items; }
    public ObjectProperty<Customer> customerProperty() { return customer; }
    public ObjectProperty<PaymentMethod> paymentMethodProperty() { return paymentMethod; }
    public ObjectProperty<BigDecimal> discountProperty() { return discount; }
    public ObjectProperty<BigDecimal> receivedProperty() { return received; }
    public StringProperty notesProperty() { return notes; }
    public ObjectProperty<BigDecimal> taxRateProperty() { return taxRate; }

    public ReadOnlyObjectProperty<BigDecimal> subtotalProperty() { return subtotal.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<BigDecimal> appliedDiscountProperty() { return appliedDiscount.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<BigDecimal> taxProperty() { return tax.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<BigDecimal> totalProperty() { return total.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<BigDecimal> changeProperty() { return change.getReadOnlyProperty(); }
    public ReadOnlyIntegerProperty unitsProperty() { return units.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty cashProperty() { return cash.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty emptyProperty() { return empty.getReadOnlyProperty(); }
    public ReadOnlyStringProperty warningProperty() { return warning.getReadOnlyProperty(); }

    public BigDecimal getSubtotal() { return subtotal.get(); }
    public BigDecimal getAppliedDiscount() { return appliedDiscount.get(); }
    public BigDecimal getTax() { return tax.get(); }
    public BigDecimal getTotal() { return total.get(); }
    public BigDecimal getChange() { return change.get(); }
    public int getUnits() { return units.get(); }
    public boolean isEmpty() { return empty.get(); }
    public String getWarning() { return warning.get(); }
}
