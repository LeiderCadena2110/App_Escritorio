package co.edu.ucc.stockflow.ui.dialog;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.model.MovementType;
import co.edu.ucc.stockflow.model.Product;
import co.edu.ucc.stockflow.model.StockMovement;
import co.edu.ucc.stockflow.service.ProductService;
import co.edu.ucc.stockflow.service.StockService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.component.FormGrid;
import co.edu.ucc.stockflow.util.Money;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Registers a purchase, a return, a loss or an adjustment of stock. */
@Component
public class StockMovementDialog {

    private final ProductService products;
    private final StockService stock;
    private final Dialogs dialogs;

    public StockMovementDialog(ProductService products, StockService stock, Dialogs dialogs) {
        this.products = products;
        this.stock = stock;
        this.dialogs = dialogs;
    }

    /**
     * @param product  product already chosen, or null to pick it in the dialog
     * @param initial  movement type selected at the start (PURCHASE if null)
     */
    public Optional<StockMovement> show(Window owner, Product product, MovementType initial) {
        FormDialog<StockMovement> fd = new FormDialog<>(dialogs, owner, "Movimiento de inventario", "Registrar");
        FormGrid form = fd.form();

        TextField search = form.add("Buscar producto", FormGrid.text(null, "Escriba el código o el nombre"));
        FilteredList<Product> options = new FilteredList<>(FXCollections.observableArrayList(products.findActive()));
        ComboBox<Product> productBox = form.add("Producto *", new ComboBox<>(options));
        productBox.setPromptText("Elija un producto");
        search.textProperty().addListener((o, old, text) -> {
            String t = text == null ? "" : text.trim().toLowerCase();
            Product selected = productBox.getValue();
            options.setPredicate(p -> t.isEmpty() || p.getSku().toLowerCase().contains(t)
                    || p.getName().toLowerCase().contains(t));
            if (selected != null && options.contains(selected)) productBox.setValue(selected);
            else if (options.size() == 1) productBox.setValue(options.get(0));
        });
        if (product != null) {
            options.stream().filter(p -> p.getId().equals(product.getId())).findFirst()
                    .ifPresent(p -> productBox.getSelectionModel().select(p));
            productBox.setDisable(true);
            search.setDisable(true);
        }

        ComboBox<MovementType> type = form.add("Tipo *", new ComboBox<>());
        type.getItems().addAll(Arrays.stream(MovementType.values()).filter(MovementType::isManual).toList());
        type.setValue(initial == null || !initial.isManual() ? MovementType.PURCHASE : initial);

        TextField quantity = form.add("Cantidad *", FormGrid.integer(1, true));
        Label quantityHint = form.add("", new Label());
        quantityHint.getStyleClass().add("muted");
        TextField unitCost = form.add("Costo unitario", FormGrid.money(null));
        TextField reference = form.add("Referencia", FormGrid.text(null, "N.º de factura del proveedor, conteo..."));
        TextField notes = form.add("Notas", FormGrid.text(null, "Opcional"));
        Label preview = form.add("Resultado", new Label());
        preview.getStyleClass().add("success-text");

        Runnable update = () -> {
            MovementType t = type.getValue();
            boolean purchase = t == MovementType.PURCHASE;
            unitCost.setDisable(!purchase);
            quantityHint.setText(t == MovementType.ADJUSTMENT
                    ? "Use un número negativo para restar (ej: -2) y positivo para sumar."
                    : t.sign() > 0 ? "Unidades que entran al inventario." : "Unidades que salen del inventario.");
            Product p = productBox.getValue();
            if (p == null) {
                preview.setText("Elija un producto");
                return;
            }
            if (purchase) unitCost.setPromptText("Costo actual: " + Money.number(p.getCostPrice()));
            int q;
            try {
                q = FormGrid.intValue(quantity);
            } catch (NumberFormatException e) {
                q = 0;
            }
            int change = t.sign() == 0 ? q : Math.abs(q) * t.sign();
            int after = p.getStock() + change;
            preview.setText("Stock: " + p.getStock() + " → " + after + " " + p.getUnit());
            preview.getStyleClass().removeAll("success-text", "error-text");
            preview.getStyleClass().add(after < 0 ? "error-text" : "success-text");
        };
        type.valueProperty().addListener((o, a, b) -> update.run());
        quantity.textProperty().addListener((o, a, b) -> update.run());
        productBox.valueProperty().addListener((o, a, b) -> update.run());
        update.run();

        return fd.show(() -> {
            Product p = productBox.getValue();
            if (p == null) throw new BusinessException("Elija un producto de la lista.");
            BigDecimal cost = unitCost.getText().isBlank() ? null : FormGrid.moneyValue(unitCost);
            return stock.register(p.getId(), type.getValue(), FormGrid.intValue(quantity), cost,
                    reference.getText(), notes.getText());
        }, product == null ? search : quantity);
    }
}
