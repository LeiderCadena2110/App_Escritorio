package co.edu.ucc.stockflow.assistant.domain;

/** How soon a product must be bought again. */
public enum RestockPriority {
    /** Sold out, or the stock lasts less than the supplier takes to deliver. */
    URGENT("Urgente"),
    /** Below the minimum stock, or it runs out before the next usual purchase. */
    SOON("Pronto");

    private final String label;

    RestockPriority(String label) {
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
