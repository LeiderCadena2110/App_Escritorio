package co.edu.ucc.stockflow.model.tax;

import java.math.BigDecimal;

/** "No responsable de IVA" (small businesses): no line pays VAT, whatever its product says. */
public final class NotVatResponsiblePolicy implements TaxPolicy {

    @Override
    public BigDecimal rateFor(BigDecimal productRate) {
        return BigDecimal.ZERO;
    }

    @Override
    public String label() {
        return "No responsable de IVA";
    }
}
