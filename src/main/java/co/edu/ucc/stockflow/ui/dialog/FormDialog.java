package co.edu.ucc.stockflow.ui.dialog;

import co.edu.ucc.stockflow.exception.BusinessException;
import co.edu.ucc.stockflow.ui.Dialogs;
import co.edu.ucc.stockflow.ui.TaskRunner;
import co.edu.ucc.stockflow.ui.component.FormGrid;
import java.util.Optional;
import java.util.concurrent.Callable;
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
    private final TaskRunner tasks;
    private Supplier<Callable<T>> onSave;

    public FormDialog(Dialogs dialogs, TaskRunner tasks, Window owner, String title, String saveText) {
        this.dialogs = dialogs;
        this.tasks = tasks;
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
            e.consume(); // the dialog closes only when the save worked
            Callable<T> work;
            try {
                work = onSave.get(); // reads the fields on the JavaFX thread
            } catch (BusinessException | NumberFormatException ex) {
                showError(ex instanceof NumberFormatException ? "Revise los números escritos." : ex.getMessage());
                return;
            }
            setSaving(true);
            tasks.submit(work, saved -> {
                dialog.setResult(saved);
                dialog.close();
            }, ex -> {
                setSaving(false);
                if (ex instanceof BusinessException) {
                    showError(ex.getMessage());
                } else {
                    dialogs.error(dialog.getDialogPane().getScene().getWindow(), "No se pudo guardar", ex);
                }
            });
        });
        dialog.setResultConverter(b -> null); // Cancel / Esc; a saved value is set with setResult
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

    /** Disables the buttons while the save runs in the background. */
    private void setSaving(boolean saving) {
        dialog.getDialogPane().getButtonTypes().forEach(t -> dialog.getDialogPane().lookupButton(t).setDisable(saving));
        dialog.getDialogPane().setCursor(saving ? javafx.scene.Cursor.WAIT : javafx.scene.Cursor.DEFAULT);
    }

    /**
     * Shows the dialog. When the user presses save, {@code saveAction} runs on the JavaFX thread to
     * read and check the fields, and returns the work that saves them, which runs in the background.
     */
    public Optional<T> show(Supplier<Callable<T>> saveAction, Node focus) {
        this.onSave = saveAction;
        if (focus != null) Platform.runLater(focus::requestFocus);
        return dialog.showAndWait();
    }

    Dialogs dialogs() {
        return dialogs;
    }
}
