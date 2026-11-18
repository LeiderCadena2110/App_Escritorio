package co.edu.ucc.stockflow.model.tax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxPolicyTest {

    @Test
    void responsibleBusinessesChargeTheRateOfEachProduct() {
        TaxPolicy policy = TaxPolicy.forRegime(true);
        assertInstanceOf(VatResponsiblePolicy.class, policy);
        assertEquals(new BigDecimal("19"), policy.rateFor(new BigDecimal("19")));
        assertEquals(new BigDecimal("5"), policy.rateFor(new BigDecimal("5")));
        assertEquals(BigDecimal.ZERO, policy.rateFor(null));
    }

    @Test
    void nonResponsibleBusinessesChargeNoVat() {
        TaxPolicy policy = TaxPolicy.forRegime(false);
        assertInstanceOf(NotVatResponsiblePolicy.class, policy);
        assertEquals(BigDecimal.ZERO, policy.rateFor(new BigDecimal("19")));
        assertEquals("No responsable de IVA", policy.label());
    }

    @Test
    void ratesAreShownWithoutTrailingZeros() {
        assertEquals("19 %", new TaxLine(new BigDecimal("19.00"), BigDecimal.ONE, BigDecimal.ONE).rateLabel());
        assertEquals("2,5 %", new TaxLine(new BigDecimal("2.50"), BigDecimal.ONE, BigDecimal.ONE).rateLabel());
    }
}
