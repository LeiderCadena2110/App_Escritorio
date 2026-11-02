package co.edu.ucc.stockflow.ui;

/** Sections of the sidebar (Ctrl/Cmd + 1…9). Each one has a view in /views/&lt;fxml&gt;.fxml. */
public enum Section {
    DASHBOARD("dashboard", "Inicio"),
    POINT_OF_SALE("point-of-sale", "Vender"),
    INVOICES("invoices", "Facturas"),
    PRODUCTS("products", "Productos"),
    INVENTORY("inventory", "Inventario"),
    CUSTOMERS("customers", "Clientes"),
    SUPPLIERS("suppliers", "Proveedores"),
    REPORTS("reports", "Reportes"),
    SETTINGS("settings", "Configuración");

    private final String fxml;
    private final String label;

    Section(String fxml, String label) {
        this.fxml = fxml;
        this.label = label;
    }

    public String fxml() {
        return fxml;
    }

    public String label() {
        return label;
    }
}
