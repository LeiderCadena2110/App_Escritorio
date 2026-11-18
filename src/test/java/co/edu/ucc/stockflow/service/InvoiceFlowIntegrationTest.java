package co.edu.ucc.stockflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.exception.ErrorCode;
import co.edu.ucc.stockflow.exception.InsufficientStockException;
import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.InvoiceStatus;
import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.util.DateRange;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Full flow against an in-memory H2 database: create products, sell, cancel and check the stock.
 * Each test runs in a transaction that is rolled back at the end.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:stockflow-test;DB_CLOSE_DELAY=-1",
        "logging.file.name="
})
@Transactional
class InvoiceFlowIntegrationTest {

    @Autowired
    private ProductService products;
    @Autowired
    private InvoiceService invoices;
    @Autowired
    private StockService stock;
    @Autowired
    private ReportService reports;
    @Autowired
    private SettingsService settings;
    @Autowired
    private SequenceService sequences;

    private Product newProduct(String sku, String price, int units) {
        Product p = new Product();
        p.setSku(sku);
        p.setName("Producto " + sku);
        p.setCostPrice(new BigDecimal("1000"));
        p.setSalePrice(new BigDecimal(price));
        p.setStock(units);
        p.setMinStock(1);
        return products.create(p);
    }

    @Test
    void sellingTakesUnitsOutOfStockAndCancellingPutsThemBack() {
        settings.setDefaultTaxRate(new BigDecimal("19"));
        Product shoe = newProduct("IT-001", "100000", 5);

        Invoice draft = new Invoice();
        draft.getItems().add(InvoiceItem.of(shoe, 2));
        draft.setPaymentMethod(PaymentMethod.CARD);
        Invoice saved = invoices.issue(draft);

        assertEquals(new BigDecimal("238000.00"), saved.getTotal());
        assertEquals(3, products.findById(shoe.getId()).orElseThrow().getStock());
        assertTrue(saved.getNumber().startsWith(sequences.invoiceSeries().prefix()));
        assertEquals(1, reports.salesSummary(DateRange.today()).invoices());

        invoices.cancel(saved.getId(), "Error en la venta");

        assertEquals(5, products.findById(shoe.getId()).orElseThrow().getStock());
        assertEquals(InvoiceStatus.CANCELLED, invoices.findById(saved.getId()).orElseThrow().getStatus());
        assertEquals(0, reports.salesSummary(DateRange.today()).invoices());
        // initial + sale + cancellation
        assertEquals(3, stock.history(shoe.getId()).size());
    }

    @Test
    void cannotSellMoreThanTheStock() {
        Product shoe = newProduct("IT-002", "50000", 1);
        Invoice draft = new Invoice();
        draft.getItems().add(InvoiceItem.of(shoe, 2));
        assertThrows(InsufficientStockException.class, () -> invoices.issue(draft));
        assertEquals(1, products.findById(shoe.getId()).orElseThrow().getStock());
    }

    @Test
    void cashMustCoverTheTotal() {
        Product shoe = newProduct("IT-003", "50000", 3);
        Invoice draft = new Invoice();
        draft.getItems().add(InvoiceItem.of(shoe, 1));
        draft.setPaymentMethod(PaymentMethod.CASH);
        draft.setAmountPaid(new BigDecimal("1000"));
        assertThrows(BusinessException.class, () -> invoices.issue(draft));
    }

    @Test
    void invoiceNumbersAreConsecutive() {
        Product shoe = newProduct("IT-004", "10000", 10);
        Invoice a = new Invoice();
        a.getItems().add(InvoiceItem.of(shoe, 1));
        Invoice b = new Invoice();
        b.getItems().add(InvoiceItem.of(shoe, 1));
        String first = invoices.issue(a).getNumber();
        String second = invoices.issue(b).getNumber();
        long n1 = Long.parseLong(first.replaceAll("\\D", ""));
        long n2 = Long.parseLong(second.replaceAll("\\D", ""));
        assertEquals(n1 + 1, n2);
    }

    @Test
    void duplicatedSkuIsRejected() {
        newProduct("IT-005", "10000", 0);
        assertThrows(BusinessException.class, () -> newProduct("it-005", "10000", 0));
    }

    @Test
    void soldProductsAreDeactivatedInsteadOfDeleted() {
        Product shoe = newProduct("IT-006", "10000", 2);
        Invoice draft = new Invoice();
        draft.getItems().add(InvoiceItem.of(shoe, 1));
        invoices.issue(draft);
        assertFalse(products.delete(shoe));
        assertFalse(products.findById(shoe.getId()).orElseThrow().isActive());
    }

    @Test
    void manualAdjustmentCannotLeaveNegativeStock() {
        Product shoe = newProduct("IT-007", "10000", 2);
        assertThrows(InsufficientStockException.class,
                () -> stock.register(shoe.getId(), MovementType.DAMAGE, 3, null, null, "Rotos"));
        stock.register(shoe.getId(), MovementType.PURCHASE, 4, new BigDecimal("1000"), "FC-1", null);
        assertEquals(6, products.findById(shoe.getId()).orElseThrow().getStock());
    }

    @Test
    void invoiceSeriesUsesTheConfiguredPrefixAndNumber() {
        sequences.configureInvoices("POS-", 500);
        Product shoe = newProduct("IT-008", "10000", 3);
        Invoice draft = new Invoice();
        draft.getItems().add(InvoiceItem.of(shoe, 1));
        assertEquals("POS-000500", invoices.issue(draft).getNumber());
        assertEquals("POS-000501", sequences.peekInvoiceNumber());
    }

    @Test
    void productsCanBeFoundByBarcodeAndBarcodesAreUnique() {
        Product shoe = newProduct("IT-009", "10000", 1);
        shoe.setBarcode("7709000000017");
        products.update(shoe);
        assertEquals(shoe.getId(), products.findByCode("7709000000017").orElseThrow().getId());
        assertEquals(shoe.getId(), products.findByCode("it-009").orElseThrow().getId());

        Product other = newProduct("IT-010", "10000", 1);
        other.setBarcode("7709000000017");
        assertThrows(BusinessException.class, () -> products.update(other));
    }

    @Test
    void productsWithStockHistoryAreDeactivatedNotDeleted() {
        Product bought = newProduct("IT-011", "10000", 0);
        stock.register(bought.getId(), MovementType.PURCHASE, 5, new BigDecimal("1000"), "FC-9", null);
        assertFalse(products.delete(bought));
        assertEquals(1, stock.history(bought.getId()).size());

        Product untouched = newProduct("IT-012", "10000", 3);
        assertTrue(products.delete(untouched));
        assertTrue(products.findById(untouched.getId()).isEmpty());
    }

    @Test
    void mixedVatInvoicesAreSavedWithTheRateOfEachLine() {
        settings.setVatResponsible(true);
        Product shoes = newProduct("IT-013", "100000", 2);
        Product book = newProduct("IT-014", "20000", 2);
        book.setTaxRate(new BigDecimal("5"));
        products.update(book);

        Invoice draft = new Invoice();
        draft.addLine(products.findById(shoes.getId()).orElseThrow(), 1);
        draft.addLine(products.findById(book.getId()).orElseThrow(), 1);
        draft.setPaymentMethod(PaymentMethod.CARD);
        Invoice saved = invoices.issue(draft);

        Invoice reloaded = invoices.findById(saved.getId()).orElseThrow();
        assertEquals(new BigDecimal("20000.00"), reloaded.getTax()); // 19.000 + 1.000
        assertEquals(2, reloaded.getTaxBreakdown().size());
        assertEquals(new BigDecimal("140000.00"), reloaded.getTotal());
    }

    @Test
    void nonResponsibleBusinessesIssueInvoicesWithoutVat() {
        settings.setVatResponsible(false);
        Invoice draft = new Invoice();
        draft.addLine(newProduct("IT-015", "50000", 1), 1);
        draft.setPaymentMethod(PaymentMethod.TRANSFER);
        Invoice saved = invoices.issue(draft);
        assertEquals(0, saved.getTax().signum());
        assertEquals(new BigDecimal("50000.00"), saved.getTotal());
    }

    @Test
    void creditSalesNeedAnIdentifiedCustomer() {
        Invoice draft = new Invoice();
        draft.addLine(newProduct("IT-016", "50000", 1), 1);
        draft.setPaymentMethod(PaymentMethod.CREDIT);
        assertEquals(ErrorCode.PAYMENT_CREDIT_REQUIRES_CUSTOMER,
                assertThrows(BusinessException.class, () -> invoices.issue(draft)).getCode());
    }
}
