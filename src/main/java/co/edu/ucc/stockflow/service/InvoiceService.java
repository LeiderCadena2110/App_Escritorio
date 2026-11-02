package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.exception.InsufficientStockException;
import co.edu.ucc.stockflow.exception.NotFoundException;
import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.InvoiceStatus;
import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.repository.InvoiceRepository;
import co.edu.ucc.stockflow.repository.ProductRepository;
import co.edu.ucc.stockflow.util.DateRange;
import co.edu.ucc.stockflow.util.Formats;
import co.edu.ucc.stockflow.util.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues and cancels sales invoices, keeping the stock in sync. */
@Service
public class InvoiceService {

    private final InvoiceRepository invoices;
    private final ProductRepository products;
    private final StockService stockService;
    private final CustomerService customerService;
    private final SettingsService settings;

    public InvoiceService(InvoiceRepository invoices, ProductRepository products, StockService stockService,
                          CustomerService customerService, SettingsService settings) {
        this.invoices = invoices;
        this.products = products;
        this.stockService = stockService;
        this.customerService = customerService;
        this.settings = settings;
    }

    /**
     * Calculates the line totals, subtotal, VAT and total of the invoice.
     * subtotal − discount = taxable base; VAT = base × rate / 100; total = base + VAT.
     */
    public static void calculateTotals(Invoice invoice, BigDecimal taxRate) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            item.recalculate();
            subtotal = subtotal.add(item.getLineTotal());
        }
        subtotal = Money.round(subtotal);
        BigDecimal discount = Money.round(invoice.getDiscount());
        if (discount.signum() < 0) discount = BigDecimal.ZERO;
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        BigDecimal base = subtotal.subtract(discount);
        BigDecimal tax = Money.percentOf(base, taxRate);
        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discount);
        invoice.setTaxRate(taxRate);
        invoice.setTax(tax);
        invoice.setTotal(base.add(tax));
    }

    /** Totals with the VAT rate of the settings (used by the point of sale while editing). */
    public void calculateTotals(Invoice invoice) {
        calculateTotals(invoice, settings.taxRate());
    }

    /**
     * Saves the invoice, takes the units out of stock and assigns the next number.
     * Nothing is saved if any product does not have enough stock.
     */
    @Transactional
    public Invoice issue(Invoice draft) {
        if (draft.getItems().isEmpty()) throw new BusinessException("Agregue al menos un producto a la factura.");
        if (draft.getDiscount() != null && draft.getDiscount().signum() < 0) {
            throw new BusinessException("El descuento no puede ser negativo.");
        }

        // 1. Check every product and the total quantity per product before writing anything
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (InvoiceItem item : draft.getItems()) {
            if (item.getQuantity() <= 0) throw new BusinessException("Las cantidades deben ser mayores que 0.");
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
            Product p = products.findById(e.getKey()).orElseThrow(() -> new NotFoundException("el producto", e.getKey()));
            if (!p.isActive()) throw new BusinessException("El producto " + p.getSku() + " está inactivo.");
            if (p.getStock() < e.getValue()) {
                throw new InsufficientStockException(p.getSku(), p.getName(), p.getStock(), e.getValue());
            }
            for (InvoiceItem item : draft.getItems()) {
                if (item.getProductId().equals(p.getId())) {
                    item.setProductSku(p.getSku());
                    item.setProductName(p.getName());
                    item.setUnitCost(p.getCostPrice());
                }
            }
        }

        // 2. Totals, customer and payment
        if (draft.getDiscount() != null) {
            BigDecimal gross = draft.getItems().stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (draft.getDiscount().compareTo(gross) > 0) {
                throw new BusinessException("El descuento no puede ser mayor que el subtotal.");
            }
        }
        calculateTotals(draft, settings.taxRate());
        if (draft.getCustomerId() == null) draft.setCustomerId(customerService.finalConsumer().getId());
        if (draft.getPaymentMethod() == null) draft.setPaymentMethod(PaymentMethod.CASH);
        if (draft.getPaymentMethod() == PaymentMethod.CASH) {
            BigDecimal paid = draft.getAmountPaid() == null ? BigDecimal.ZERO : Money.round(draft.getAmountPaid());
            if (paid.signum() == 0) paid = draft.getTotal();
            if (paid.compareTo(draft.getTotal()) < 0) {
                throw new BusinessException("El dinero recibido (" + Money.format(paid)
                        + ") es menor que el total (" + Money.format(draft.getTotal()) + ").");
            }
            draft.setAmountPaid(paid);
        } else if (draft.getPaymentMethod() == PaymentMethod.CREDIT) {
            draft.setAmountPaid(BigDecimal.ZERO);
        } else {
            draft.setAmountPaid(draft.getTotal());
        }
        draft.setNotes(Formats.clean(draft.getNotes()));
        draft.setStatus(InvoiceStatus.PAID);
        draft.setIssuedAt(LocalDateTime.now());
        draft.setNumber(reserveNumber());

        // 3. Save and take the units out of stock
        Invoice saved = invoices.insert(draft);
        for (InvoiceItem item : saved.getItems()) {
            stockService.register(item.getProductId(), MovementType.SALE, item.getQuantity(), item.getUnitCost(),
                    saved.getNumber(), null);
        }
        return saved;
    }

    /** Cancels the invoice and puts the units back in stock. */
    @Transactional
    public Invoice cancel(long invoiceId, String reason) {
        Invoice invoice = invoices.findById(invoiceId).orElseThrow(() -> new NotFoundException("la factura", invoiceId));
        if (invoice.isCancelled()) throw new BusinessException("La factura " + invoice.getNumber() + " ya está anulada.");
        String why = Formats.clean(reason);
        if (why == null) throw new BusinessException("Escriba el motivo de la anulación.");
        for (InvoiceItem item : invoice.getItems()) {
            stockService.register(item.getProductId(), MovementType.SALE_CANCELLATION, item.getQuantity(),
                    item.getUnitCost(), invoice.getNumber(), why);
        }
        LocalDateTime now = LocalDateTime.now();
        invoices.markCancelled(invoiceId, now, "[Anulada: " + why + "]");
        return invoices.findById(invoiceId).orElseThrow();
    }

    public Optional<Invoice> findById(long id) {
        return invoices.findById(id);
    }

    public List<Invoice> search(DateRange range, String text, InvoiceStatus status) {
        return invoices.search(range == null ? null : range.start(), range == null ? null : range.end(), text, status);
    }

    /** Next free number (skips numbers that already exist) and moves the counter forward. */
    private String reserveNumber() {
        String prefix = settings.invoicePrefix();
        long next = settings.nextInvoiceNumber();
        String number = SettingsService.formatInvoiceNumber(prefix, next);
        while (invoices.existsNumber(number)) {
            next++;
            number = SettingsService.formatInvoiceNumber(prefix, next);
        }
        settings.setNextInvoiceNumber(next + 1);
        return number;
    }
}
