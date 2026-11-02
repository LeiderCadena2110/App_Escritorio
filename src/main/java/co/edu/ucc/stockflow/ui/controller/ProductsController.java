package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.model.Category;
import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.service.CategoryService;
import co.edu.ucc.stockflow.service.CsvExportService;
import co.edu.ucc.stockflow.service.ProductService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.ViewLoader;
import co.edu.ucc.stockflow.ui.component.Combos;
import co.edu.ucc.stockflow.ui.component.FileChoosers;
import co.edu.ucc.stockflow.ui.component.Tables;
import co.edu.ucc.stockflow.ui.dialog.CategoryManagerDialog;
import co.edu.ucc.stockflow.ui.dialog.ProductDialog;
import co.edu.ucc.stockflow.ui.dialog.StockMovementDialog;
import co.edu.ucc.stockflow.util.Money;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ProductsController implements SectionController {

    @FXML private TextField searchField;
    @FXML private ComboBox<Category> categoryFilter;
    @FXML private CheckBox lowStockOnly;
    @FXML private CheckBox showInactive;
    @FXML private TableView<Product> table;
    @FXML private Button editButton;
    @FXML private MenuButton stockButton;
    @FXML private Button historyButton;
    @FXML private Button sellButton;
    @FXML private Button deleteButton;
    @FXML private Label summaryLabel;

    private final ProductService products;
    private final CategoryService categories;
    private final CsvExportService csv;
    private final ProductDialog productDialog;
    private final StockMovementDialog movementDialog;
    private final CategoryManagerDialog categoryDialog;
    private final Dialogs dialogs;
    private final Navigator navigator;
    private final ViewLoader views;
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(250));

    public ProductsController(ProductService products, CategoryService categories, CsvExportService csv,
                              ProductDialog productDialog, StockMovementDialog movementDialog,
                              CategoryManagerDialog categoryDialog, Dialogs dialogs, Navigator navigator,
                              ViewLoader views) {
        this.products = products;
        this.categories = categories;
        this.csv = csv;
        this.productDialog = productDialog;
        this.movementDialog = movementDialog;
        this.categoryDialog = categoryDialog;
        this.dialogs = dialogs;
        this.navigator = navigator;
        this.views = views;
    }

    @FXML
    private void initialize() {
        table.getColumns().setAll(List.of(
                Tables.text("Código", Product::getSku, 100),
                Tables.text("Nombre", Product::getName, 250),
                Tables.text("Categoría", Product::getCategoryName, 130),
                Tables.money("Costo", Product::getCostPrice, 105),
                Tables.money("Precio", Product::getSalePrice, 105),
                Tables.text("Margen", p -> p.getMarginPercent().toPlainString().replace('.', ',') + " %", 75),
                Tables.integer("Stock", Product::getStock, 70),
                Tables.integer("Mín.", Product::getMinStock, 60),
                Tables.text("Unidad", Product::getUnit, 65),
                Tables.badge("Estado", ProductsController::statusText, ProductsController::statusStyle, 110)));
        Tables.fill(table, 1);
        Tables.placeholder(table, "No hay productos con esos filtros. Cree uno con \"Nuevo producto\".");
        Tables.styledRows(table, p -> !p.isActive() ? "row-inactive" : p.isLowStock() ? "row-low-stock" : null,
                this::edit);

        var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        stockButton.disableProperty().bind(noSelection);
        historyButton.disableProperty().bind(noSelection);
        sellButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);

        Combos.nullText(categoryFilter, "Todas las categorías");
        searchDelay.setOnFinished(e -> reload());
        searchField.textProperty().addListener((o, a, b) -> searchDelay.playFromStart());
        categoryFilter.valueProperty().addListener((o, a, b) -> reload());
        lowStockOnly.selectedProperty().addListener((o, a, b) -> reload());
        showInactive.selectedProperty().addListener((o, a, b) -> reload());
    }

    @Override
    public void onShow() {
        reloadCategories();
        reload();
    }

    private void reloadCategories() {
        Category selected = categoryFilter.getValue();
        categoryFilter.getItems().setAll((Category) null);
        categoryFilter.getItems().addAll(categories.findAll());
        categoryFilter.setValue(selected != null && categoryFilter.getItems().contains(selected) ? selected : null);
    }

    private void reload() {
        Product selected = table.getSelectionModel().getSelectedItem();
        Category c = categoryFilter.getValue();
        List<Product> list = products.search(searchField.getText(), c == null ? null : c.getId(),
                lowStockOnly.isSelected(), showInactive.isSelected());
        table.getItems().setAll(list);
        if (selected != null) table.getSelectionModel().select(selected);
        BigDecimal value = list.stream().filter(Product::isActive).map(Product::getStockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long low = list.stream().filter(Product::isLowStock).count();
        summaryLabel.setText(list.size() + " productos · " + low + " con stock bajo · valor " + Money.format(value));
    }

    private Product selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void newProduct() {
        productDialog.show(navigator.window(), null).ifPresent(p -> {
            reload();
            table.getSelectionModel().select(p);
            table.scrollTo(p);
        });
    }

    @FXML
    private void editSelected() {
        if (selected() != null) edit(selected());
    }

    private void edit(Product p) {
        products.findById(p.getId()).ifPresent(fresh ->
                productDialog.show(navigator.window(), fresh).ifPresent(saved -> reload()));
    }

    @FXML
    private void purchase() {
        movement(MovementType.PURCHASE);
    }

    @FXML
    private void adjust() {
        movement(MovementType.ADJUSTMENT);
    }

    @FXML
    private void damage() {
        movement(MovementType.DAMAGE);
    }

    private void movement(MovementType type) {
        if (selected() == null) return;
        movementDialog.show(navigator.window(), selected(), type).ifPresent(m -> reload());
    }

    @FXML
    private void history() {
        if (selected() != null) navigator.showHistory(selected());
    }

    @FXML
    private void sell() {
        Product p = selected();
        if (p == null) return;
        if (!p.isActive()) {
            dialogs.info(navigator.window(), "Producto inactivo", "Active el producto para poder venderlo.");
            return;
        }
        navigator.sell(p);
    }

    @FXML
    private void deleteSelected() {
        Product p = selected();
        if (p == null) return;
        if (!dialogs.confirm(navigator.window(), "Eliminar producto",
                "¿Eliminar " + p.getSku() + " · " + p.getName() + "?\n\n"
                        + "Si el producto ya tiene ventas, solo se desactivará para conservar el historial.",
                "Eliminar")) {
            return;
        }
        dialogs.run(navigator.window(), "No se pudo eliminar", () -> {
            boolean deleted = products.delete(p);
            if (!deleted) {
                dialogs.info(navigator.window(), "Producto desactivado",
                        p.getName() + " tiene ventas registradas, así que se desactivó en lugar de borrarse.\n"
                                + "Puede verlo marcando \"Mostrar inactivos\".");
            }
            reload();
        });
    }

    @FXML
    private void manageCategories() {
        categoryDialog.show(navigator.window());
        reloadCategories();
        reload();
    }

    @FXML
    private void exportCsv() {
        Path file = FileChoosers.saveCsv(navigator.window(), "productos");
        if (file == null) return;
        dialogs.run(navigator.window(), "No se pudo exportar", () -> {
            try {
                csv.exportProducts(table.getItems(), file);
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            views.openFile(file);
        });
    }

    private static String statusText(Product p) {
        if (!p.isActive()) return "Inactivo";
        if (p.getStock() == 0) return "Agotado";
        if (p.isLowStock()) return "Stock bajo";
        return "Disponible";
    }

    private static String statusStyle(Product p) {
        if (!p.isActive()) return "badge-danger";
        if (p.getStock() == 0) return "badge-danger";
        if (p.isLowStock()) return "badge-warning";
        return "badge-ok";
    }
}
