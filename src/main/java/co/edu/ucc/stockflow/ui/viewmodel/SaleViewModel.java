package co.edu.ucc.stockflow.ui.viewmodel;

import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.model.tax.TaxPolicy;
import co.edu.ucc.stockflow.model.tax.VatResponsiblePolicy;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.util.Formats;
import co.edu.ucc.stockflow.shared.util.Money;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * State of the sale being built in the point of sale (MVVM view model).
 *
 * <p>The rules (stock, discount, VAT) belong to the {@link Invoice} aggregate; this class keeps a
 * draft invoice, exposes it as JavaFX properties for the screen, and recalculates it whenever
 * something changes. It knows nothing about controls, so it is tested with plain JUnit.</p>
 */
public class SaleViewModel {

    private final ObservableList<InvoiceItem> items = FXCollections.observableArrayList();
    private final ObjectProperty<Customer> customer = new SimpleObjectProperty<>();
    private final ObjectProperty<PaymentMethod> paymentMethod = new SimpleObjectProperty<>(PaymentMethod.CASH);
    private final ObjectProperty<BigDecimal> discount = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final ObjectProperty<BigDecimal> received = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final StringProperty notes = new SimpleStringProperty("");
    private final ObjectProperty<TaxPolicy> taxPolicy = new SimpleObjectProperty<>(new VatResponsiblePolicy());

    private final ReadOnlyObjectWrapper<BigDecimal> subtotal = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> appliedDiscount = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> tax = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> total = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyObjectWrapper<BigDecimal> change = new ReadOnlyObjectWrapper<>(BigDecimal.ZERO);
    private final ReadOnlyIntegerWrapper units = new ReadOnlyIntegerWrapper();
    private final ReadOnlyBooleanWrapper cash = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyBooleanWrapper empty = new ReadOnlyBooleanWrapper(true);
    private final ReadOnlyStringWrapper warning = new ReadOnlyStringWrapper("");
    private final ReadOnlyStringWrapper taxLabel = new ReadOnlyStringWrapper("IVA");

    /** The invoice being built: it owns the lines and the rules. */
    private final Invoice draft = new Invoice();

    /** Latest data of each product in the catalog, to check the stock. */
    private final Map<Long, Product> catalog = new HashMap<>();
    /** Message of the last action that failed (shown until something else changes). */
    private String notice = "";

    public SaleViewModel() {
        Observable[] inputs = {items, customer, paymentMethod, discount, received, taxPolicy};
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
     * @throws co.edu.ucc.stockflow.exception.InsufficientStockException if there is not enough stock
     */
    public InvoiceItem add(Product product) {
        InvoiceItem line = draft.addLine(catalog.getOrDefault(product.getId(), product), 1);
        sync();
        return line;
    }

    /** Changes the quantity of a line; 0 or less removes it. */
    public void setQuantity(InvoiceItem item, int quantity) {
        draft.changeQuantity(item, quantity, catalog.get(item.getProductId()));
        sync();
    }

    public void increase(InvoiceItem item) {
        setQuantity(item, item.getQuantity() + 1);
    }

    public void decrease(InvoiceItem item) {
        setQuantity(item, item.getQuantity() - 1);
    }

    public void remove(InvoiceItem item) {
        draft.removeLine(item);
        sync();
    }

    /** Empties the sale and goes back to cash payment, without discount or notes. */
    public void clear(Customer defaultCustomer) {
        for (InvoiceItem line : List.copyOf(draft.getItems())) draft.removeLine(line);
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

    /** The invoice ready to be issued: a copy of the draft, so the screen keeps its own. */
    public Invoice toInvoice() {
        fillDraft();
        return draft.copy();
    }

    /** Copies what the user chose (customer, payment, discount...) into the draft. */
    private void fillDraft() {
        draft.setCustomerId(customer.get() == null ? null : customer.get().getId());
        draft.setPaymentMethod(paymentMethod.get());
        draft.setDiscount(discount.get() == null ? BigDecimal.ZERO : discount.get());
        draft.setAmountPaid(paymentMethod.get() == PaymentMethod.CASH && received.get() != null
                ? received.get() : BigDecimal.ZERO);
        draft.setNotes(Formats.clean(notes.get()));
    }

    /** Shows the lines of the draft in the observable list (the table refreshes). */
    private void sync() {
        items.setAll(draft.getItems());
    }

    // ------------------------------------------------------------------ totals

    private void recalculate() {
        fillDraft();
        BigDecimal requested = draft.getDiscount();
        draft.calculate(taxPolicy.get() == null ? new VatResponsiblePolicy() : taxPolicy.get());
        subtotal.set(draft.getSubtotal());
        appliedDiscount.set(draft.getDiscount());
        tax.set(draft.getTax());
        total.set(draft.getTotal());
        units.set(draft.getItemCount());
        empty.set(draft.isEmpty());
        cash.set(paymentMethod.get() == PaymentMethod.CASH);
        taxLabel.set(draft.singleTaxRate().map(r -> "IVA (" + r.toPlainString().replace('.', ',') + " %)").orElse("IVA"));

        BigDecimal paid = received.get() == null ? BigDecimal.ZERO : received.get();
        BigDecimal difference = paid.subtract(draft.getTotal());
        change.set(cash.get() && paid.signum() > 0 && difference.signum() > 0 ? difference : BigDecimal.ZERO);

        String w = "";
        if (!draft.isEmpty() && requested.compareTo(draft.getSubtotal()) > 0) {
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
    public ObjectProperty<TaxPolicy> taxPolicyProperty() { return taxPolicy; }

    public ReadOnlyObjectProperty<BigDecimal> subtotalProperty() { return subtotal.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<BigDecimal> appliedDiscountProperty() { return appliedDiscount.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<BigDecimal> taxProperty() { return tax.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<BigDecimal> totalProperty() { return total.getReadOnlyProperty(); }
    public ReadOnlyObjectProperty<BigDecimal> changeProperty() { return change.getReadOnlyProperty(); }
    public ReadOnlyIntegerProperty unitsProperty() { return units.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty cashProperty() { return cash.getReadOnlyProperty(); }
    public ReadOnlyBooleanProperty emptyProperty() { return empty.getReadOnlyProperty(); }
    public ReadOnlyStringProperty warningProperty() { return warning.getReadOnlyProperty(); }
    public ReadOnlyStringProperty taxLabelProperty() { return taxLabel.getReadOnlyProperty(); }

    public BigDecimal getSubtotal() { return subtotal.get(); }
    public BigDecimal getAppliedDiscount() { return appliedDiscount.get(); }
    public BigDecimal getTax() { return tax.get(); }
    public BigDecimal getTotal() { return total.get(); }
    public BigDecimal getChange() { return change.get(); }
    public int getUnits() { return units.get(); }
    public boolean isEmpty() { return empty.get(); }
    public String getWarning() { return warning.get(); }
}
