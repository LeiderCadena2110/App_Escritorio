package co.edu.ucc.stockflow.dto;

/** Business data printed on the invoices. */
public record BusinessInfo(String name, String taxId, String address, String phone, String email,
                           String invoiceFooter) {
}
