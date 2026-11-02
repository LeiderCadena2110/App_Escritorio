package co.edu.ucc.stockflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.edu.ucc.stockflow.model.Invoice;
import co.edu.ucc.stockflow.model.InvoiceItem;
import co.edu.ucc.stockflow.model.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InvoiceTotalsTest {

    private static Product product(long id, String price) {
        Product p = new Product();
        p.setId(id);
        p.setSku("P" + id);
        p.setName("Producto " + id);
        p.setSalePrice(new BigDecimal(price));
        p.setCostPrice(BigDecimal.ZERO);
        return p;
    }

    @Test
    void appliesDiscountBeforeVat() {
        Invoice inv = new Invoice();
        inv.getItems().add(InvoiceItem.of(product(1, "229900"), 2));
        inv.getItems().add(InvoiceItem.of(product(2, "18900"), 3));
        inv.setDiscount(new BigDecimal("10000"));

        InvoiceService.calculateTotals(inv, new BigDecimal("19"));

        assertEquals(new BigDecimal("516500.00"), inv.getSubtotal());
        assertEquals(new BigDecimal("10000.00"), inv.getDiscount());
        assertEquals(new BigDecimal("96235.00"), inv.getTax());
        assertEquals(new BigDecimal("602735.00"), inv.getTotal());
    }

    @Test
    void withoutVatTheTotalIsTheBase() {
        Invoice inv = new Invoice();
        inv.getItems().add(InvoiceItem.of(product(1, "50000"), 1));
        InvoiceService.calculateTotals(inv, BigDecimal.ZERO);
        assertEquals(0, inv.getTax().signum());
        assertEquals(new BigDecimal("50000.00"), inv.getTotal());
    }

    @Test
    void discountIsLimitedToTheSubtotal() {
        Invoice inv = new Invoice();
        inv.getItems().add(InvoiceItem.of(product(1, "10000"), 1));
        inv.setDiscount(new BigDecimal("99999"));
        InvoiceService.calculateTotals(inv, new BigDecimal("19"));
        assertEquals(new BigDecimal("10000.00"), inv.getDiscount());
        assertEquals(new BigDecimal("0.00"), inv.getTotal());
    }

    @Test
    void changeIsNeverNegative() {
        Invoice inv = new Invoice();
        inv.setTotal(new BigDecimal("50000"));
        inv.setAmountPaid(new BigDecimal("40000"));
        assertEquals(BigDecimal.ZERO, inv.getChange());
        inv.setAmountPaid(new BigDecimal("100000"));
        assertEquals(new BigDecimal("50000"), inv.getChange());
    }

    @Test
    void invoiceNumbersHaveSixDigits() {
        assertEquals("FV-000042", SettingsService.formatInvoiceNumber("FV-", 42));
        assertEquals("1234567", SettingsService.formatInvoiceNumber(null, 1234567));
    }
}
