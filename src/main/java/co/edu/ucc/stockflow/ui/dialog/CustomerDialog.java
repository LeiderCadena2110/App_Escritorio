package co.edu.ucc.stockflow.ui.dialog;

import co.edu.ucc.stockflow.model.Customer;
import co.edu.ucc.stockflow.service.CustomerService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.component.FormGrid;
import java.util.Optional;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Create / edit customer form. */
@Component
public class CustomerDialog {

    private final CustomerService customers;
    private final Dialogs dialogs;

    public CustomerDialog(CustomerService customers, Dialogs dialogs) {
        this.customers = customers;
        this.dialogs = dialogs;
    }

    /** @param customer null to create a new one */
    public Optional<Customer> show(Window owner, Customer customer) {
        boolean isNew = customer == null;
        Customer c = isNew ? new Customer() : customer;
        FormDialog<Customer> fd = new FormDialog<>(dialogs, owner, isNew ? "Nuevo cliente" : "Editar cliente",
                isNew ? "Crear cliente" : "Guardar cambios");
        FormGrid form = fd.form();
        ComboBox<String> type = form.add("Tipo de documento", new ComboBox<>());
        type.getItems().addAll(CustomerService.DOCUMENT_TYPES);
        type.setValue(c.getDocumentType());
        TextField document = form.add("Número de documento *", FormGrid.text(c.getDocumentNumber(), "Sin puntos"));
        TextField name = form.add("Nombre o razón social *", FormGrid.text(c.getName(), ""));
        TextField phone = form.add("Teléfono", FormGrid.text(c.getPhone(), ""));
        TextField email = form.add("Correo", FormGrid.text(c.getEmail(), "nombre@correo.com"));
        TextField address = form.add("Dirección", FormGrid.text(c.getAddress(), ""));
        return fd.show(() -> {
            c.setDocumentType(type.getValue());
            c.setDocumentNumber(document.getText());
            c.setName(name.getText());
            c.setPhone(phone.getText());
            c.setEmail(email.getText());
            c.setAddress(address.getText());
            return customers.save(c);
        }, isNew ? document : name);
    }
}
