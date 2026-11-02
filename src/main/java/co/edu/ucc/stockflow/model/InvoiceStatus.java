package co.edu.ucc.stockflow.model;

/** An issued invoice is PAID; a cancelled one keeps its number but no longer counts as a sale. */
public enum InvoiceStatus {
    PAID("Pagada"),
    CANCELLED("Anulada");

    private final String label;

    InvoiceStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
