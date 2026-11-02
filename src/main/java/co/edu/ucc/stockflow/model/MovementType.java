package co.edu.ucc.stockflow.model;

/**
 * Reason for a stock change. The sign says whether units come in (+1) or go out (-1);
 * an adjustment can go either way, so its quantity carries the sign.
 */
public enum MovementType {
    INITIAL("Inventario inicial", 1),
    PURCHASE("Entrada por compra", 1),
    CUSTOMER_RETURN("Devolución de cliente", 1),
    SALE("Venta", -1),
    SALE_CANCELLATION("Anulación de venta", 1),
    SUPPLIER_RETURN("Devolución a proveedor", -1),
    DAMAGE("Pérdida o daño", -1),
    ADJUSTMENT("Ajuste de inventario", 0);

    private final String label;
    private final int sign;

    MovementType(String label, int sign) {
        this.label = label;
        this.sign = sign;
    }

    public String label() {
        return label;
    }

    /** +1 adds stock, -1 removes it, 0 means the quantity already has its sign. */
    public int sign() {
        return sign;
    }

    /** Types the user can register by hand (sales and cancellations come from invoices). */
    public boolean isManual() {
        return this != SALE && this != SALE_CANCELLATION && this != INITIAL;
    }

    @Override
    public String toString() {
        return label;
    }
}
