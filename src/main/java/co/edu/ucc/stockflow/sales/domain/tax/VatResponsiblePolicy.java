package co.edu.ucc.stockflow.sales.domain.tax;

import java.math.BigDecimal;

/** "Responsable de IVA": each line pays the VAT of its product (19 %, 5 % or 0 % exempt). */
public final class VatResponsiblePolicy implements TaxPolicy {

    @Override
    public BigDecimal rateFor(BigDecimal productRate) {
        return productRate == null ? BigDecimal.ZERO : productRate;
    }

    @Override
    public String label() {
        return "Responsable de IVA";
    }
}
