package co.edu.ucc.stockflow.model.tax;

import java.math.BigDecimal;

/** VAT of one rate inside an invoice: the taxable base (after discount) and the tax charged. */
public record TaxLine(BigDecimal rate, BigDecimal base, BigDecimal tax) {

    /** "19 %", "5 %", "0 %". */
    public String rateLabel() {
        return rate.stripTrailingZeros().toPlainString().replace('.', ',') + " %";
    }
}
