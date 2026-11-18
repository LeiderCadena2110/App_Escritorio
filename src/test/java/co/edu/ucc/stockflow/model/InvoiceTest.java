package co.edu.ucc.stockflow.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.exception.ErrorCode;
import co.edu.ucc.stockflow.exception.InsufficientStockException;
import co.edu.ucc.stockflow.model.tax.NotVatResponsiblePolicy;
import co.edu.ucc.stockflow.model.tax.TaxLine;
import co.edu.ucc.stockflow.model.tax.VatResponsiblePolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The invoice aggregate keeps its lines and totals consistent by itself. */
class InvoiceTest {

    private static Product product(long id, String price, String vat, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setSku("P" + id);
        p.setName("Producto " + id);
        p.setSalePrice(new BigDecimal(price));
        p.setCostPrice(BigDecimal.ONE);
        p.setTaxRate(new BigDecimal(vat));
        p.setStock(stock);
        p.setUnit("UND");
        return p;
    }

    @Test
    void addingAProductTwiceMergesTheLineAndChecksTheStock() {
        Invoice inv = new Invoice();
        Product shoes = product(1, "229900", "19", 3);
        inv.addLine(shoes, 2);
        inv.addLine(shoes, 1);
        assertEquals(1, inv.getItems().size());
        assertEquals(3, inv.getItems().get(0).getQuantity());
        assertThrows(InsufficientStockException.class, () -> inv.addLine(shoes, 1));
        assertEquals(3, inv.getItems().get(0).getQuantity());
    }

    @Test
    void changingTheQuantityToZeroRemovesTheLine() {
        Invoice inv = new Invoice();
        Product socks = product(2, "18900", "19", 10);
        InvoiceItem line = inv.addLine(socks, 4);
        inv.changeQuantity(line, 11 - 1, socks);
        assertEquals(10, line.getQuantity());
        assertThrows(InsufficientStockException.class, () -> inv.changeQuantity(line, 11, socks));
        inv.changeQuantity(line, 0, socks);
        assertTrue(inv.isEmpty());
    }

    @Test
    void singleRateTotalsMatchTheClassicFormula() {
        Invoice inv = new Invoice();
        inv.addLine(product(1, "229900", "19", 5), 2);
        inv.addLine(product(2, "18900", "19", 5), 3);
        inv.setDiscount(new BigDecimal("10000"));
        inv.calculate(new VatResponsiblePolicy());

        assertEquals(new BigDecimal("516500.00"), inv.getSubtotal());
        assertEquals(new BigDecimal("96235.00"), inv.getTax());
        assertEquals(new BigDecimal("602735.00"), inv.getTotal());
        assertEquals(Optional.of(new BigDecimal("19")), inv.singleTaxRate());
    }

    @Test
    void mixedRatesSpreadTheDiscountAndBreakDownTheVat() {
        Invoice inv = new Invoice();
        inv.addLine(product(1, "100000", "19", 5), 1);
        inv.addLine(product(2, "20000", "5", 5), 1);
        inv.addLine(product(3, "10000", "0", 5), 1);
        inv.setDiscount(new BigDecimal("13000")); // 10 % of 130.000
        inv.calculate(new VatResponsiblePolicy());

        List<TaxLine> vat = inv.getTaxBreakdown();
        assertEquals(3, vat.size());
        assertEquals(new TaxLine(new BigDecimal("19"), new BigDecimal("90000.00"), new BigDecimal("17100.00")), vat.get(0));
        assertEquals(new TaxLine(new BigDecimal("5"), new BigDecimal("18000.00"), new BigDecimal("900.00")), vat.get(1));
        assertEquals(new TaxLine(BigDecimal.ZERO, new BigDecimal("9000.00"), new BigDecimal("0.00")), vat.get(2));
        assertEquals(new BigDecimal("18000.00"), inv.getTax());
        assertEquals(new BigDecimal("135000.00"), inv.getTotal()); // 130.000 − 13.000 + 18.000
        assertTrue(inv.singleTaxRate().isEmpty());
    }

    @Test
    void theDiscountSpreadAlwaysAddsUpToTheDiscount() {
        Invoice inv = new Invoice();
        inv.addLine(product(1, "33333", "19", 5), 1);
        inv.addLine(product(2, "33333", "5", 5), 1);
        inv.addLine(product(3, "33334", "0", 5), 1);
        inv.setDiscount(new BigDecimal("10001"));
        inv.calculate(new VatResponsiblePolicy());
        BigDecimal bases = inv.getTaxBreakdown().stream().map(TaxLine::base).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(inv.getSubtotal().subtract(inv.getDiscount()), bases);
    }

    @Test
    void nonResponsibleBusinessesChargeNoVatAndLinesRememberTheProductRate() {
        Invoice inv = new Invoice();
        InvoiceItem line = inv.addLine(product(1, "50000", "19", 5), 1);
        inv.calculate(new NotVatResponsiblePolicy());
        assertEquals(0, inv.getTax().signum());
        assertEquals(new BigDecimal("50000.00"), inv.getTotal());
        assertEquals(new BigDecimal("19"), line.getProductTaxRate());

        inv.calculate(new VatResponsiblePolicy());
        assertEquals(new BigDecimal("9500.00"), inv.getTax());
    }

    @Test
    void issuingValidatesTheDiscountWhileThePreviewCapsIt() {
        Invoice inv = new Invoice();
        inv.addLine(product(1, "10000", "19", 5), 1);
        inv.setDiscount(new BigDecimal("99999"));
        assertEquals(ErrorCode.INVOICE_DISCOUNT_ABOVE_SUBTOTAL,
                assertThrows(BusinessException.class, inv::validateDiscount).getCode());
        inv.calculate(new VatResponsiblePolicy());
        assertEquals(new BigDecimal("10000.00"), inv.getDiscount());
        assertEquals(0, inv.getTotal().signum());
    }

    @Test
    void copiesDoNotShareLines() {
        Invoice inv = new Invoice();
        inv.addLine(product(1, "10000", "19", 5), 2);
        Invoice copy = inv.copy();
        assertNotSame(inv.getItems().get(0), copy.getItems().get(0));
        copy.getItems().get(0).setQuantity(1);
        assertEquals(2, inv.getItems().get(0).getQuantity());
    }

    @Test
    void negativeUnitsAreRejected() {
        assertEquals(ErrorCode.INVOICE_QUANTITY_NOT_POSITIVE, assertThrows(BusinessException.class,
                () -> new Invoice().addLine(product(1, "1", "19", 5), 0)).getCode());
    }
}
