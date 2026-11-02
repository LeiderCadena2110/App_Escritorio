package co.edu.ucc.stockflow.ui.controller;

import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.service.CustomerService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.Navigator;
import co.edu.ucc.stockflow.ui.SectionController;
import co.edu.ucc.stockflow.ui.component.Tables;
import co.edu.ucc.stockflow.ui.dialog.CustomerDialog;
import co.edu.ucc.stockflow.util.Formats;
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
public class CustomersController implements SectionController {

    @FXML private TextField searchField;
    @FXML private TableView<Customer> table;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Label summaryLabel;

    private final CustomerService customers;
    private final CustomerDialog dialog;
    private final Dialogs dialogs;
    private final Navigator navigator;
    private final PauseTransition searchDelay = new PauseTransition(Duration.millis(250));

    public CustomersController(CustomerService customers, CustomerDialog dialog, Dialogs dialogs, Navigator navigator) {
        this.customers = customers;
        this.dialog = dialog;
        this.dialogs = dialogs;
        this.navigator = navigator;
    }

    @FXML
    private void initialize() {
        table.getColumns().setAll(List.of(
                Tables.text("Documento", c -> c.getDocumentType() + " " + c.getDocumentNumber(), 150),
                Tables.text("Nombre", Customer::getName, 240),
                Tables.text("Teléfono", Customer::getPhone, 130),
                Tables.text("Correo", Customer::getEmail, 210),
                Tables.text("Dirección", Customer::getAddress, 200),
                Tables.text("Desde", c -> c.getCreatedAt() == null ? "" : Formats.date(c.getCreatedAt().toLocalDate()), 100)));
        Tables.fill(table, 1);
        Tables.placeholder(table, "No hay clientes con esa búsqueda.");
        Tables.onOpen(table, this::edit);
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
        table.getItems().setAll(customers.search(searchField.getText()));
        summaryLabel.setText(table.getItems().size() + " clientes");
    }

    @FXML
    private void create() {
        dialog.show(navigator.window(), null).ifPresent(c -> {
            reload();
            table.getSelectionModel().select(c);
        });
    }

    @FXML
    private void editSelected() {
        Customer c = table.getSelectionModel().getSelectedItem();
        if (c != null) edit(c);
    }

    private void edit(Customer c) {
        dialog.show(navigator.window(), c).ifPresent(saved -> reload());
    }

    @FXML
    private void deleteSelected() {
        Customer c = table.getSelectionModel().getSelectedItem();
        if (c == null) return;
        if (dialogs.confirm(navigator.window(), "Eliminar cliente", "¿Eliminar a " + c.getName() + "?", "Eliminar")) {
            dialogs.run(navigator.window(), "No se pudo eliminar", () -> {
                customers.delete(c);
                reload();
            });
        }
    }
}
