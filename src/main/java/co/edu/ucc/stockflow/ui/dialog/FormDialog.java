package co.edu.ucc.stockflow.ui.dialog;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.component.FormGrid;
import java.util.Optional;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Edit dialog with a form, a "Guardar" button and an error line. When saving fails because of a
 * business rule, the dialog stays open and shows the message so the user can fix the data.
 */
public class FormDialog<T> {

    private final Dialog<T> dialog = new Dialog<>();
    private final FormGrid form = new FormGrid();
    private final Label error = new Label();
    private final ButtonType save;
    private final Dialogs dialogs;
    private Supplier<T> onSave;
    private T result;

    public FormDialog(Dialogs dialogs, Window owner, String title, String saveText) {
        this.dialogs = dialogs;
        dialogs.prepare(dialog, owner, title);
        save = new ButtonType(saveText, ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(save, new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE));
        error.getStyleClass().add("error-text");
        error.setWrapText(true);
        error.setMaxWidth(440);
        error.setManaged(false);
        error.setVisible(false);
        VBox box = new VBox(12, form, error);
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().setPrefWidth(540);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(save);
        saveButton.getStyleClass().add("primary-button");
        saveButton.addEventFilter(ActionEvent.ACTION, e -> {
            try {
                result = onSave.get();
            } catch (BusinessException | NumberFormatException ex) {
                showError(ex instanceof NumberFormatException ? "Revise los números escritos." : ex.getMessage());
                e.consume();
            } catch (RuntimeException ex) {
                dialogs.error(dialog.getDialogPane().getScene().getWindow(), "No se pudo guardar", ex);
                e.consume();
            }
        });
        dialog.setResultConverter(b -> b == save ? result : null);
    }

    public FormGrid form() {
        return form;
    }

    public Dialog<T> dialog() {
        return dialog;
    }

    public void showError(String message) {
        error.setText(message);
        error.setManaged(true);
        error.setVisible(true);
        dialog.getDialogPane().getScene().getWindow().sizeToScene();
    }

    /** Shows the dialog; the action runs when the user presses the save button. */
    public Optional<T> show(Supplier<T> saveAction, Node focus) {
        this.onSave = saveAction;
        if (focus != null) Platform.runLater(focus::requestFocus);
        return dialog.showAndWait();
    }

    Dialogs dialogs() {
        return dialogs;
    }
}
