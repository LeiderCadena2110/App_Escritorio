package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.model.StockMovement;
import co.edu.ucc.stockflow.service.CsvExportService;
import co.edu.ucc.stockflow.service.ProductService;
import co.edu.ucc.stockflow.service.StockService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.ViewLoader;
import co.edu.ucc.stockflow.ui.component.Combos;
import co.edu.ucc.stockflow.ui.component.DatePickers;
import co.edu.ucc.stockflow.ui.component.FileChoosers;
import co.edu.ucc.stockflow.ui.component.Tables;
import co.edu.ucc.stockflow.ui.dialog.StockMovementDialog;
import co.edu.ucc.stockflow.util.DateRange;
import co.edu.ucc.stockflow.util.Money;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class InventoryController implements SectionController {

    @FXML private ComboBox<Product> productBox;
    @FXML private ComboBox<MovementType> typeBox;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private TableView<StockMovement> table;
    @FXML private Label productInfoLabel;
    @FXML private Label summaryLabel;

    private final StockService stock;
    private final ProductService products;
    private final CsvExportService csv;
    private final StockMovementDialog movementDialog;
    private final Dialogs dialogs;
    private final Navigator navigator;
    private final ViewLoader views;
    private boolean loading;

    public InventoryController(StockService stock, ProductService products, CsvExportService csv,
                               StockMovementDialog movementDialog, Dialogs dialogs, Navigator navigator,
                               ViewLoader views) {
        this.stock = stock;
        this.products = products;
        this.csv = csv;
        this.movementDialog = movementDialog;
        this.dialogs = dialogs;
        this.navigator = navigator;
        this.views = views;
    }

    @FXML
    private void initialize() {
        TableColumn<StockMovement, Integer> quantity = new TableColumn<>("Cantidad");
        quantity.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getQuantity()));
        quantity.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer q, boolean empty) {
                super.updateItem(q, empty);
                setText(empty || q == null ? null : (q > 0 ? "+" + q : q.toString()));
            }
        });
        quantity.getStyleClass().addAll("numeric-column", "quantity-cell");
        quantity.setPrefWidth(80);

        table.getColumns().setAll(List.of(
                Tables.dateTime("Fecha", StockMovement::getCreatedAt, 135),
                Tables.text("Código", StockMovement::getProductSku, 95),
                Tables.text("Producto", StockMovement::getProductName, 220),
                Tables.text("Tipo", m -> m.getType().label(), 160),
                quantity,
                Tables.integer("Stock final", StockMovement::getStockAfter, 85),
                Tables.money("Costo unit.", StockMovement::getUnitCost, 105),
                Tables.text("Referencia", StockMovement::getReference, 110),
                Tables.text("Notas", StockMovement::getNotes, 180)));
        Tables.fill(table, 2);
        Tables.placeholder(table, "No hay movimientos con esos filtros.");
        Tables.styledRows(table, m -> m.isEntry() ? "row-entry" : "row-exit", null);

        Combos.nullText(productBox, "Todos los productos");
        typeBox.getItems().setAll((MovementType) null);
        typeBox.getItems().addAll(MovementType.values());
        Combos.nullText(typeBox, "Todos los tipos");

        DatePickers.dayMonthYear(fromDate, toDate);
        DateRange last30 = DateRange.lastDays(30);
        fromDate.setValue(last30.start().toLocalDate());
        toDate.setValue(LocalDate.now());

        productBox.valueProperty().addListener((o, a, b) -> reload());
        typeBox.valueProperty().addListener((o, a, b) -> reload());
        fromDate.valueProperty().addListener((o, a, b) -> reload());
        toDate.valueProperty().addListener((o, a, b) -> reload());
    }

    @Override
    public void onShow() {
        reloadProducts();
        reload();
    }

    /** Shows the whole history of one product (called from Products and the dashboard). */
    public void filterByProduct(Product product) {
        loading = true;
        reloadProducts();
        productBox.getItems().stream().filter(p -> p != null && p.getId().equals(product.getId()))
                .findFirst().ifPresent(productBox::setValue);
        typeBox.setValue(null);
        fromDate.setValue(null);
        toDate.setValue(null);
        loading = false;
        reload();
    }

    private void reloadProducts() {
        boolean wasLoading = loading;
        loading = true;
        Product selected = productBox.getValue();
        productBox.getItems().setAll((Product) null);
        productBox.getItems().addAll(products.search(null, null, false, true));
        productBox.setValue(selected == null ? null : productBox.getItems().stream()
                .filter(p -> p != null && p.getId().equals(selected.getId())).findFirst().orElse(null));
        loading = wasLoading;
    }

    private void reload() {
        if (loading) return;
        LocalDate from = fromDate.getValue() == null ? LocalDate.of(2000, 1, 1) : fromDate.getValue();
        LocalDate to = toDate.getValue() == null ? LocalDate.now().plusYears(1) : toDate.getValue();
        Product p = productBox.getValue();
        List<StockMovement> list = stock.search(DateRange.ofDays(from, to), p == null ? null : p.getId(), typeBox.getValue());
        table.getItems().setAll(list);
        int in = list.stream().filter(StockMovement::isEntry).mapToInt(StockMovement::getQuantity).sum();
        int out = list.stream().filter(m -> !m.isEntry()).mapToInt(StockMovement::getQuantity).sum();
        summaryLabel.setText(list.size() + " movimientos · entradas +" + in + " · salidas " + out);
        if (p == null) {
            productInfoLabel.setText("");
        } else {
            products.findById(p.getId()).ifPresent(fresh -> productInfoLabel.setText(fresh.getSku() + " · "
                    + fresh.getName() + ": stock actual " + fresh.getStock() + " " + fresh.getUnit()
                    + " (mínimo " + fresh.getMinStock() + ") · costo promedio " + Money.format(fresh.getCostPrice())));
        }
    }

    @FXML
    private void newPurchase() {
        movementDialog.show(navigator.window(), productBox.getValue(), MovementType.PURCHASE).ifPresent(m -> onShow());
    }

    @FXML
    private void newAdjustment() {
        movementDialog.show(navigator.window(), productBox.getValue(), MovementType.ADJUSTMENT).ifPresent(m -> onShow());
    }

    @FXML
    private void clearFilters() {
        loading = true;
        productBox.setValue(null);
        typeBox.setValue(null);
        fromDate.setValue(null);
        toDate.setValue(null);
        loading = false;
        reload();
    }

    @FXML
    private void exportCsv() {
        Path file = FileChoosers.saveCsv(navigator.window(), "movimientos-inventario");
        if (file == null) return;
        dialogs.run(navigator.window(), "No se pudo exportar", () -> {
            try {
                csv.exportMovements(table.getItems(), file);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            views.openFile(file);
        });
    }
}
