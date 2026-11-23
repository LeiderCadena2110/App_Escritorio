package co.edu.ucc.stockflow.shared.ui;

/** Sections of the sidebar (Ctrl/Cmd + 1…9). Each one has a view in /views/&lt;fxml&gt;.fxml. */
public enum Section {
    DASHBOARD("dashboard", "Inicio", Icon.DASHBOARD),
    POINT_OF_SALE("point-of-sale", "Vender", Icon.CART),
    INVOICES("invoices", "Facturas", Icon.RECEIPT),
    PRODUCTS("products", "Productos", Icon.TAG),
    INVENTORY("inventory", "Inventario", Icon.LAYERS),
    CUSTOMERS("customers", "Clientes", Icon.PEOPLE),
    SUPPLIERS("suppliers", "Proveedores", Icon.TRUCK),
    REPORTS("reports", "Reportes", Icon.CHART),
    SETTINGS("settings", "Configuración", Icon.SETTINGS);

    private final String fxml;
    private final String label;
    private final Icon icon;

    Section(String fxml, String label, Icon icon) {
        this.fxml = fxml;
        this.label = label;
        this.icon = icon;
    }

    public Icon icon() {
        return icon;
    }

    public String fxml() {
        return fxml;
    }

    public String label() {
        return label;
    }
}
