package co.edu.ucc.stockflow.sales.domain.tax;

import java.math.BigDecimal;

/**
 * Strategy that decides which VAT rate applies to each invoice line.
 *
 * <p>In Colombia a business is either "responsable de IVA" (it charges the VAT of each product:
 * 19 %, 5 % or exempt) or "no responsable de IVA" (it charges no VAT at all). Each regime is one
 * implementation, so supporting another rule means adding a class, not editing the invoice.</p>
 */
public interface TaxPolicy {

    /** Rate (percentage, for example 19) that a line pays, given the rate configured in its product. */
    BigDecimal rateFor(BigDecimal productRate);

    /** Short text for the invoice, for example "Responsable de IVA". */
    String label();

    static TaxPolicy forRegime(boolean vatResponsible) {
        return vatResponsible ? new VatResponsiblePolicy() : new NotVatResponsiblePolicy();
    }
}
