package co.edu.ucc.stockflow.exception;

/** Trying to take out more units than there are in stock. */
public class InsufficientStockException extends BusinessException {

    private final String sku;
    private final int available;
    private final int requested;

    public InsufficientStockException(String sku, String name, int available, int requested) {
        super("No hay stock suficiente de " + sku + " · " + name + ": hay " + available
                + " y se necesitan " + requested + ".");
        this.sku = sku;
        this.available = available;
        this.requested = requested;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}
