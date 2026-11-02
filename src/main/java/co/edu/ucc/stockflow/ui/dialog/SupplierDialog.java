package co.edu.ucc.stockflow.ui.dialog;

import co.edu.ucc.stockflow.model.Supplier;
import co.edu.ucc.stockflow.service.SupplierService;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.component.FormGrid;
import java.util.Optional;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Create / edit supplier form. */
@Component
public class SupplierDialog {

    private final SupplierService suppliers;
    private final Dialogs dialogs;

    public SupplierDialog(SupplierService suppliers, Dialogs dialogs) {
        this.suppliers = suppliers;
        this.dialogs = dialogs;
    }

    /** @param supplier null to create a new one */
    public Optional<Supplier> show(Window owner, Supplier supplier) {
        boolean isNew = supplier == null;
        Supplier s = isNew ? new Supplier() : supplier;
        FormDialog<Supplier> fd = new FormDialog<>(dialogs, owner, isNew ? "Nuevo proveedor" : "Editar proveedor",
                isNew ? "Crear proveedor" : "Guardar cambios");
        FormGrid form = fd.form();
        TextField name = form.add("Nombre o razón social *", FormGrid.text(s.getName(), ""));
        TextField taxId = form.add("NIT", FormGrid.text(s.getTaxId(), "900123456-1"));
        TextField contact = form.add("Persona de contacto", FormGrid.text(s.getContactName(), ""));
        TextField phone = form.add("Teléfono", FormGrid.text(s.getPhone(), ""));
        TextField email = form.add("Correo", FormGrid.text(s.getEmail(), ""));
        TextField address = form.add("Dirección", FormGrid.text(s.getAddress(), ""));
        CheckBox active = new CheckBox("Proveedor activo");
        active.setSelected(s.isActive());
        if (!isNew) form.add("", active);
        return fd.show(() -> {
            s.setName(name.getText());
            s.setTaxId(taxId.getText());
            s.setContactName(contact.getText());
            s.setPhone(phone.getText());
            s.setEmail(email.getText());
            s.setAddress(address.getText());
            s.setActive(active.isSelected());
            return suppliers.save(s);
        }, name);
    }
}
