package co.edu.ucc.stockflow.ui;

import co.edu.ucc.stockflow.exception.BusinessException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Modal dialogs with the app theme and clear texts. Esc closes them. */
@Component
public class Dialogs {

    private final ThemeManager theme;

    public Dialogs(ThemeManager theme) {
        this.theme = theme;
    }

    public void info(Window owner, String title, String message) {
        show(Alert.AlertType.INFORMATION, owner, title, message, null);
    }

    public void error(Window owner, String title, String message) {
        show(Alert.AlertType.ERROR, owner, title, message, null);
    }

    /**
     * Shows an error. Business rule errors only show their message; unexpected errors also
     * include the technical details in an expandable area.
     */
    public void error(Window owner, String title, Throwable e) {
        if (e instanceof BusinessException) {
            show(Alert.AlertType.WARNING, owner, title, e.getMessage(), null);
        } else {
            show(Alert.AlertType.ERROR, owner, title, clearMessage(e), e);
        }
    }

    /** Runs the action and shows any error in a dialog. Returns true if it finished without errors. */
    public boolean run(Window owner, String errorTitle, Runnable action) {
        try {
            action.run();
            return true;
        } catch (RuntimeException e) {
            error(owner, errorTitle, e);
            return false;
        }
    }

    public boolean confirm(Window owner, String title, String message, String yesText) {
        ButtonType yes = new ButtonType(yesText, ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, yes, no);
        prepare(a, owner, title);
        return a.showAndWait().filter(b -> b == yes).isPresent();
    }

    public Optional<String> askText(Window owner, String title, String label, String initial) {
        TextInputDialog d = new TextInputDialog(initial);
        if (owner != null) d.initOwner(owner);
        d.setTitle(title);
        d.setHeaderText(title);
        d.setContentText(label);
        theme.apply(d.getDialogPane());
        return d.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
    }

    /** Applies owner and theme to a custom dialog. */
    public void prepare(Dialog<?> d, Window owner, String title) {
        if (owner != null) d.initOwner(owner);
        d.setTitle(title);
        d.setHeaderText(title);
        theme.apply(d.getDialogPane());
    }

    private void show(Alert.AlertType type, Window owner, String title, String message, Throwable detail) {
        Alert a = new Alert(type, message, ButtonType.OK);
        prepare(a, owner, title);
        a.getDialogPane().setMinWidth(440);
        if (detail != null) {
            TextArea text = new TextArea(stackTrace(detail));
            text.setEditable(false);
            text.setPrefRowCount(10);
            a.getDialogPane().setExpandableContent(text);
        }
        a.showAndWait();
    }

    public static String clearMessage(Throwable e) {
        Throwable t = e;
        while ((t.getMessage() == null || t.getMessage().isBlank()) && t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String m = t.getMessage();
        if (m == null || m.isBlank()) return "Error inesperado (" + t.getClass().getSimpleName() + ").";
        if (t instanceof java.nio.file.AccessDeniedException) return "No hay permiso para escribir en " + m;
        if (m.contains("being used by another process") || m.contains("está siendo utilizado")) {
            return "El archivo está abierto en otro programa. Ciérrelo e intente de nuevo.";
        }
        return m;
    }

    private static String stackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
