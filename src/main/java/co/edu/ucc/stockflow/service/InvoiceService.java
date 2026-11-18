package co.edu.ucc.stockflow.service;

import co.edu.ucc.stockflow.catalog.domain.Product;
import co.edu.ucc.stockflow.catalog.infrastructure.ProductRepository;
import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.InvoiceStatus;
import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.repository.InvoiceRepository;
import co.edu.ucc.stockflow.service.payment.PaymentPolicies;
import co.edu.ucc.stockflow.settings.application.SequenceService;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import co.edu.ucc.stockflow.shared.domain.ErrorCode;
import co.edu.ucc.stockflow.shared.domain.NotFoundException;
import co.edu.ucc.stockflow.shared.util.DateRange;
import co.edu.ucc.stockflow.shared.util.Formats;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues and cancels sales invoices, keeping the stock in sync. */
@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoices;
    private final ProductRepository products;
    private final StockService stockService;
    private final CustomerService customerService;
    private final TaxPolicyProvider taxPolicies;
    private final SequenceService sequences;
    private final PaymentPolicies payments;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoices, ProductRepository products, StockService stockService,
                          CustomerService customerService, TaxPolicyProvider taxPolicies, SequenceService sequences,
                          PaymentPolicies payments, Clock clock) {
        this.invoices = invoices;
        this.products = products;
        this.stockService = stockService;
        this.customerService = customerService;
        this.taxPolicies = taxPolicies;
        this.sequences = sequences;
        this.payments = payments;
        this.clock = clock;
    }

    /**
     * Saves the invoice, takes the units out of stock and assigns the next number.
     * Everything is checked before writing, and it all runs in one transaction: if anything fails,
     * nothing is saved.
     */
    @Transactional
    public Invoice issue(Invoice draft) {
        if (draft.isEmpty()) throw new BusinessException(ErrorCode.INVOICE_EMPTY);
        draft.validateDiscount();

        // 1. Latest data of each product: it must be active and have the units (the one stock rule)
        for (Map.Entry<Long, Integer> e : draft.unitsByProduct().entrySet()) {
            Product p = products.findById(e.getKey())
                    .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, e.getKey()));
            if (!p.isActive()) throw new BusinessException(ErrorCode.PRODUCT_INACTIVE, p.getSku());
            p.checkCanTakeOut(e.getValue());
            draft.getItems().stream().filter(i -> i.getProductId().equals(p.getId())).forEach(i -> i.refreshFrom(p));
        }

        // 2. Totals with the VAT regime of the business
        draft.calculate(taxPolicies.current());

        // 3. Customer and payment (each payment method has its own policy)
        Customer customer = draft.getCustomerId() == null ? customerService.finalConsumer()
                : customerService.findById(draft.getCustomerId());
        draft.setCustomerId(customer.getId());
        if (draft.getPaymentMethod() == null) draft.setPaymentMethod(PaymentMethod.CASH);
        draft.setAmountPaid(payments.forMethod(draft.getPaymentMethod())
                .settle(draft.getTotal(), draft.getAmountPaid(), customer));

        draft.setNotes(Formats.clean(draft.getNotes()));
        draft.setStatus(InvoiceStatus.PAID);
        draft.setIssuedAt(LocalDateTime.now(clock));
        draft.setNumber(sequences.reserveInvoiceNumber(invoices::existsNumber));

        // 4. Save and take the units out of stock
        Invoice saved = invoices.insert(draft);
        for (InvoiceItem item : saved.getItems()) {
            stockService.register(item.getProductId(), MovementType.SALE, item.getQuantity(), item.getUnitCost(),
                    saved.getNumber(), null);
        }
        log.info("Invoice {} issued: {} lines, total {}, {}", saved.getNumber(), saved.getItems().size(),
                saved.getTotal(), saved.getPaymentMethod());
        return saved;
    }

    /** Cancels the invoice and puts the units back in stock. */
    @Transactional
    public Invoice cancel(long invoiceId, String reason) {
        Invoice invoice = invoices.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.INVOICE_NOT_FOUND, invoiceId));
        if (invoice.isCancelled()) {
            throw new BusinessException(ErrorCode.INVOICE_ALREADY_CANCELLED, invoice.getNumber());
        }
        String why = Formats.clean(reason);
        if (why == null) throw new BusinessException(ErrorCode.INVOICE_CANCEL_REASON_REQUIRED);
        for (InvoiceItem item : invoice.getItems()) {
            stockService.register(item.getProductId(), MovementType.SALE_CANCELLATION, item.getQuantity(),
                    item.getUnitCost(), invoice.getNumber(), why);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        invoices.markCancelled(invoiceId, now, "[Anulada: " + why + "]");
        log.info("Invoice {} cancelled: {}", invoice.getNumber(), why);
        return invoices.findById(invoiceId).orElseThrow();
    }

    public Optional<Invoice> findById(long id) {
        return invoices.findById(id);
    }

    public List<Invoice> search(DateRange range, String text, InvoiceStatus status) {
        return invoices.search(range == null ? null : range.start(), range == null ? null : range.end(), text, status);
    }
}
