package co.edu.ucc.stockflow.ui.dialog;

import co.edu.ucc.stockflow.model.Category;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.model.Supplier;
import co.edu.ucc.stockflow.service.CategoryService;
import co.edu.ucc.stockflow.service.ProductService;
import co.edu.ucc.stockflow.service.SupplierService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.component.Combos;
import co.edu.ucc.stockflow.ui.component.FormGrid;
import co.edu.ucc.stockflow.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Create / edit product form. */
@Component
public class ProductDialog {

    private static final String[] UNITS = {"UND", "PAR", "CAJA", "PAQ", "KG", "M"};

    private final ProductService products;
    private final CategoryService categories;
    private final SupplierService suppliers;
    private final Dialogs dialogs;

    public ProductDialog(ProductService products, CategoryService categories, SupplierService suppliers,
                         Dialogs dialogs) {
        this.products = products;
        this.categories = categories;
        this.suppliers = suppliers;
        this.dialogs = dialogs;
    }

    /** @param product null to create a new one */
    public Optional<Product> show(Window owner, Product product) {
        boolean isNew = product == null;
        Product p = isNew ? new Product() : product;
        FormDialog<Product> fd = new FormDialog<>(dialogs, owner,
                isNew ? "Nuevo producto" : "Editar " + p.getSku(), isNew ? "Crear producto" : "Guardar cambios");
        FormGrid form = fd.form();

        TextField sku = form.add("Código (SKU) *", FormGrid.text(p.getSku(), "Ej: DEP-001"));
        TextField name = form.add("Nombre *", FormGrid.text(p.getName(), "Ej: Tenis running talla 40"));

        ComboBox<Category> category = form.add("Categoría", new ComboBox<>());
        category.getItems().add(null);
        category.getItems().addAll(categories.findAll());
        Combos.nullText(category, "Sin categoría");
        category.getItems().stream().filter(c -> c != null && c.getId().equals(p.getCategoryId()))
                .findFirst().ifPresent(category::setValue);

        ComboBox<Supplier> supplier = form.add("Proveedor", new ComboBox<>());
        supplier.getItems().add(null);
        supplier.getItems().addAll(suppliers.findActive());
        Combos.nullText(supplier, "Sin proveedor");
        supplier.getItems().stream().filter(s -> s != null && s.getId().equals(p.getSupplierId()))
                .findFirst().ifPresent(supplier::setValue);

        ComboBox<String> unit = form.add("Unidad", new ComboBox<>());
        unit.getItems().addAll(UNITS);
        unit.setEditable(true);
        unit.setValue(p.getUnit() == null ? "UND" : p.getUnit());

        TextField cost = form.add("Precio de costo", FormGrid.money(isNew ? null : p.getCostPrice()));
        TextField price = form.add("Precio de venta *", FormGrid.money(isNew ? null : p.getSalePrice()));
        Label margin = form.add("Margen", new Label());
        margin.getStyleClass().add("muted");
        Runnable updateMargin = () -> margin.setText(marginText(cost.getText(), price.getText()));
        cost.textProperty().addListener((o, a, b) -> updateMargin.run());
        price.textProperty().addListener((o, a, b) -> updateMargin.run());
        updateMargin.run();

        TextField stock = null;
        if (isNew) {
            stock = form.add("Stock inicial", FormGrid.integer(0, false));
        } else {
            Label current = form.add("Stock actual", new Label(p.getStock() + " " + p.getUnit()
                    + "   (se cambia desde Inventario)"));
            current.getStyleClass().add("muted");
        }
        TextField minStock = form.add("Stock mínimo", FormGrid.integer(p.getMinStock(), false));
        TextArea description = form.add("Descripción", new TextArea(p.getDescription()));
        description.setPrefRowCount(3);
        description.setWrapText(true);
        CheckBox active = new CheckBox("Producto activo (se puede vender)");
        active.setSelected(p.isActive());
        if (!isNew) form.add("", active);

        TextField initialStock = stock;
        return fd.show(() -> {
            p.setSku(sku.getText());
            p.setName(name.getText());
            p.setCategoryId(category.getValue() == null ? null : category.getValue().getId());
            p.setSupplierId(supplier.getValue() == null ? null : supplier.getValue().getId());
            p.setUnit(unit.getValue() == null ? "UND" : unit.getValue().trim().toUpperCase());
            p.setCostPrice(FormGrid.moneyValue(cost));
            p.setSalePrice(FormGrid.moneyValue(price));
            p.setMinStock(FormGrid.intValue(minStock));
            p.setDescription(description.getText());
            if (isNew) {
                p.setStock(FormGrid.intValue(initialStock));
                return products.create(p);
            }
            p.setActive(active.isSelected());
            return products.update(p);
        }, sku);
    }

    private static String marginText(String costText, String priceText) {
        BigDecimal cost = Money.parseOr(costText, BigDecimal.ZERO);
        BigDecimal price = Money.parseOr(priceText, BigDecimal.ZERO);
        if (price.signum() <= 0) return "—";
        BigDecimal profit = price.subtract(cost);
        BigDecimal percent = profit.multiply(BigDecimal.valueOf(100)).divide(price, 1, RoundingMode.HALF_UP);
        return Money.format(profit) + " por unidad (" + percent.toPlainString().replace('.', ',') + " %)";
    }
}
