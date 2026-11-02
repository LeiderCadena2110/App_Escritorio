package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.model.Supplier;
import co.edu.ucc.stockflow.service.SupplierService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.component.Tables;
import co.edu.ucc.stockflow.ui.dialog.SupplierDialog;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SuppliersController implements SectionController {

    @FXML private TextField searchField;
    @FXML private TableView<Supplier> table;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Label summaryLabel;

    private final SupplierService suppliers;
    private final SupplierDialog dialog;
    private final Dialogs dialogs;
    private final Navigator navigator;
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(250));

    public SuppliersController(SupplierService suppliers, SupplierDialog dialog, Dialogs dialogs, Navigator navigator) {
        this.suppliers = suppliers;
        this.dialog = dialog;
        this.dialogs = dialogs;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        table.getColumns().setAll(List.of(
                Tables.text("NIT", Supplier::getTaxId, 130),
                Tables.text("Nombre", Supplier::getName, 240),
                Tables.text("Contacto", Supplier::getContactName, 170),
                Tables.text("Teléfono", Supplier::getPhone, 130),
                Tables.text("Correo", Supplier::getEmail, 200),
                Tables.badge("Estado", s -> s.isActive() ? "Activo" : "Inactivo",
                        s -> s.isActive() ? "badge-ok" : "badge-danger", 100)));
        Tables.fill(table, 1);
        Tables.placeholder(table, "No hay proveedores con esa búsqueda.");
        Tables.styledRows(table, s -> s.isActive() ? null : "row-inactive", this::edit);
        var none = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(none);
        deleteButton.disableProperty().bind(none);
        searchDelay.setOnFinished(e -> reload());
        searchField.textProperty().addListener((o, a, b) -> searchDelay.playFromStart());
    }

    @Override
    public void onShow() {
        reload();
    }

    private void reload() {
        table.getItems().setAll(suppliers.search(searchField.getText()));
        summaryLabel.setText(table.getItems().size() + " proveedores");
    }

    @FXML
    private void create() {
        dialog.show(navigator.window(), null).ifPresent(s -> {
            reload();
            table.getSelectionModel().select(s);
        });
    }

    @FXML
    private void editSelected() {
        Supplier s = table.getSelectionModel().getSelectedItem();
        if (s != null) edit(s);
    }

    private void edit(Supplier s) {
        dialog.show(navigator.window(), s).ifPresent(saved -> reload());
    }

    @FXML
    private void deleteSelected() {
        Supplier s = table.getSelectionModel().getSelectedItem();
        if (s == null) return;
        if (!dialogs.confirm(navigator.window(), "Eliminar proveedor", "¿Eliminar a " + s.getName() + "?\n\n"
                + "Si tiene productos asociados, solo se desactivará.", "Eliminar")) {
            return;
        }
        dialogs.run(navigator.window(), "No se pudo eliminar", () -> {
            if (!suppliers.delete(s)) {
                dialogs.info(navigator.window(), "Proveedor desactivado",
                        s.getName() + " tiene productos asociados, así que se desactivó en lugar de borrarse.");
            }
            reload();
        });
    }
}
