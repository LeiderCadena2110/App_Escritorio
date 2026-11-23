package co.edu.ucc.stockflow.assistant.domain;

/** How sure the app is that an invoice line is a given product. */
public enum MatchQuality {
    /** The code on the invoice is the product's SKU or barcode. */
    CODE("Por código"),
    /** The description contains the product's name (or most of its words). */
    NAME("Por nombre"),
    /** No product looks like it: the user must choose one or skip the line. */
    NONE("Elegir producto");

    private final String label;

    MatchQuality(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
