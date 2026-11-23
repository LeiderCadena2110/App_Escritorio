package co.edu.ucc.stockflow.assistant.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * What the AI read from a supplier's invoice (photo or PDF). Empty texts mean "not legible".
 *
 * @param supplierName  who sold
 * @param supplierTaxId supplier's NIT
 * @param number        invoice number
 * @param issueDate     date as written, ideally YYYY-MM-DD
 * @param lines         the products bought
 * @param total         total of the invoice, to compare with the lines
 */
public record SupplierInvoice(String supplierName, String supplierTaxId, String number, String issueDate,
                              List<Line> lines, BigDecimal total) {

    public SupplierInvoice {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /**
     * One product line.
     *
     * @param code        supplier's code or reference (may be empty)
     * @param description product as written on the invoice
     * @param quantity    units bought
     * @param unitCost    price per unit before VAT
     */
    public record Line(String code, String description, int quantity, BigDecimal unitCost) {
    }
}
