package co.edu.ucc.stockflow.ui.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.PaymentMethod;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.model.tax.NotVatResponsiblePolicy;
import co.edu.ucc.stockflow.model.tax.VatResponsiblePolicy;
import co.edu.ucc.stockflow.shared.domain.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The cart rules work without opening any window. */
class SaleViewModelTest {

    private SaleViewModel sale;
    private Product shoes;
    private Product socks;

    private static Product product(long id, String sku, String price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName("Producto " + sku);
        p.setSalePrice(new BigDecimal(price));
        p.setCostPrice(BigDecimal.ONE);
        p.setStock(stock);
        p.setUnit("PAR");
        return p;
    }

    @BeforeEach
    void setUp() {
        sale = new SaleViewModel();
        sale.taxPolicyProperty().set(new VatResponsiblePolicy());
        shoes = product(1, "DEP-001", "229900", 2);
        socks = product(2, "ACC-001", "18900", 10);
        sale.updateCatalog(List.of(shoes, socks));
    }

    @Test
    void addingTheSameProductTwiceIncreasesTheQuantity() {
        sale.add(shoes);
        sale.add(shoes);
        sale.add(socks);
        assertEquals(2, sale.getItems().size());
        assertEquals(2, sale.getItems().get(0).getQuantity());
        assertEquals(3, sale.getUnits());
        assertFalse(sale.isEmpty());
    }

    @Test
    void totalsAreRecalculatedAutomatically() {
        sale.add(shoes);
        sale.add(shoes);
        sale.add(socks);
        sale.add(socks);
        sale.add(socks);
        sale.discountProperty().set(new BigDecimal("10000"));

        assertEquals(new BigDecimal("516500.00"), sale.getSubtotal());
        assertEquals(new BigDecimal("10000.00"), sale.getAppliedDiscount());
        assertEquals(new BigDecimal("96235.00"), sale.getTax());
        assertEquals(new BigDecimal("602735.00"), sale.getTotal());
    }

    @Test
    void cannotAddMoreThanTheStock() {
        sale.add(shoes);
        sale.add(shoes);
        BusinessException e = assertThrows(BusinessException.class, () -> sale.add(shoes));
        assertTrue(e.getMessage().contains("Solo hay 2"));
        assertEquals(2, sale.getItems().get(0).getQuantity());
    }

    @Test
    void soldOutProductsCannotBeAdded() {
        Product soldOut = product(3, "X", "1000", 0);
        sale.updateCatalog(List.of(soldOut));
        BusinessException e = assertThrows(BusinessException.class, () -> sale.add(soldOut));
        assertTrue(e.getMessage().contains("agotado"));
    }

    @Test
    void decreasingToZeroRemovesTheLine() {
        InvoiceItem line = sale.add(socks);
        sale.decrease(line);
        assertTrue(sale.isEmpty());
        assertEquals(0, sale.getTotal().signum());
    }

    @Test
    void setQuantityChecksTheStock() {
        InvoiceItem line = sale.add(socks);
        sale.setQuantity(line, 7);
        assertEquals(7, sale.getUnits());
        assertThrows(BusinessException.class, () -> sale.setQuantity(line, 11));
        assertEquals(7, sale.getUnits());
    }

    @Test
    void cashShowsChangeOrWhatIsMissing() {
        sale.add(socks); // 18.900 + 19 % = 22.491
        sale.receivedProperty().set(new BigDecimal("50000"));
        assertEquals(new BigDecimal("27509.00"), sale.getChange());
        assertEquals("", sale.getWarning());

        sale.receivedProperty().set(new BigDecimal("20000"));
        assertEquals(0, sale.getChange().signum());
        assertTrue(sale.getWarning().startsWith("Faltan $ 2.491"));
    }

    @Test
    void otherPaymentMethodsIgnoreTheCashReceived() {
        sale.add(socks);
        sale.receivedProperty().set(new BigDecimal("1000"));
        sale.paymentMethodProperty().set(PaymentMethod.CARD);
        assertFalse(sale.cashProperty().get());
        assertEquals("", sale.getWarning());
        assertEquals(0, sale.toInvoice().getAmountPaid().signum());
    }

    @Test
    void warnsWhenTheDiscountIsBiggerThanTheSubtotal() {
        sale.add(socks);
        sale.discountProperty().set(new BigDecimal("50000"));
        assertEquals("El descuento es mayor que el subtotal.", sale.getWarning());
    }

    @Test
    void noticesStayUntilTheNextChange() {
        sale.showNotice("No se encontró \"zzz\".");
        assertEquals("No se encontró \"zzz\".", sale.getWarning());
        sale.add(socks);
        assertEquals("", sale.getWarning());
    }

    @Test
    void toInvoiceCopiesTheLinesAndPayment() {
        sale.add(shoes);
        sale.notesProperty().set("  Cliente frecuente ");
        Invoice inv = sale.toInvoice();
        assertEquals(1, inv.getItems().size());
        assertEquals(PaymentMethod.CASH, inv.getPaymentMethod());
        assertEquals("Cliente frecuente", inv.getNotes());
        inv.getItems().get(0).setQuantity(99);
        assertEquals(1, sale.getItems().get(0).getQuantity());
    }

    @Test
    void clearGoesBackToAnEmptyCashSale() {
        sale.add(shoes);
        sale.paymentMethodProperty().set(PaymentMethod.TRANSFER);
        sale.discountProperty().set(BigDecimal.TEN);
        sale.clear(null);
        assertTrue(sale.isEmpty());
        assertEquals(PaymentMethod.CASH, sale.paymentMethodProperty().get());
        assertEquals(0, sale.discountProperty().get().signum());
    }

    @Test
    void theVatLabelShowsTheRateOnlyWhenThereIsOne() {
        sale.add(shoes);
        assertEquals("IVA (19 %)", sale.taxLabelProperty().get());
        Product book = product(4, "BOOK", "20000", 3);
        book.setTaxRate(new BigDecimal("5"));
        sale.add(book);
        assertEquals("IVA", sale.taxLabelProperty().get());
    }

    @Test
    void changingTheTaxRegimeRecalculatesTheTotals() {
        sale.add(socks);
        assertEquals(new BigDecimal("22491.00"), sale.getTotal());
        sale.taxPolicyProperty().set(new NotVatResponsiblePolicy());
        assertEquals(new BigDecimal("18900.00"), sale.getTotal());
    }
}
