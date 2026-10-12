package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.i18n.Textos;
import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Diálogos modales con el tema de la app y textos claros. Esc los cierra. */
@Component
public class Dialogos {

    private final Tema tema;

    public Dialogos(Tema tema) {
        this.tema = tema;
    }

    public void info(Window duenio, String titulo, String mensaje) {
        mostrar(Alert.AlertType.INFORMATION, duenio, titulo, mensaje, null);
    }

    public void error(Window duenio, String titulo, String mensaje) {
        mostrar(Alert.AlertType.ERROR, duenio, titulo, mensaje, null);
    }

    /** Error con detalle técnico desplegable (para el soporte). */
    public void error(Window duenio, String titulo, Throwable e) {
        mostrar(Alert.AlertType.ERROR, duenio, titulo, mensajeClaro(e), e);
    }

    public boolean confirmar(Window duenio, String titulo, String mensaje, String textoSi) {
        ButtonType si = new ButtonType(textoSi, ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType(Textos.t("comun.cancelar"), ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, si, no);
        preparar(a, duenio, titulo);
        return a.showAndWait().filter(b -> b == si).isPresent();
    }

    public Optional<String> pedirTexto(Window duenio, String titulo, String etiqueta, String inicial) {
        TextInputDialog d = new TextInputDialog(inicial);
        d.initOwner(duenio);
        d.setTitle(titulo);
        d.setHeaderText(null);
        d.setContentText(etiqueta);
        tema.aplicar(d.getDialogPane());
        return d.showAndWait().map(String::trim).filter(s -> !s.isEmpty());
    }

    private void mostrar(Alert.AlertType tipo, Window duenio, String titulo, String mensaje, Throwable detalle) {
        Alert a = new Alert(tipo, mensaje, ButtonType.OK);
        preparar(a, duenio, titulo);
        if (detalle != null) {
            TextArea texto = new TextArea(pila(detalle));
            texto.setEditable(false);
            texto.setWrapText(false);
            texto.setPrefRowCount(10);
            a.getDialogPane().setExpandableContent(texto);
        }
        a.showAndWait();
    }

    private void preparar(Alert a, Window duenio, String titulo) {
        if (duenio != null) a.initOwner(duenio);
        a.setTitle(titulo);
        a.setHeaderText(titulo);
        a.getDialogPane().setMinWidth(460);
        tema.aplicar(a.getDialogPane());
    }

    public static String mensajeClaro(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            if (t.getMessage() != null && !t.getMessage().isBlank() && !(t instanceof java.lang.reflect.InvocationTargetException)) break;
            t = t.getCause();
        }
        String m = t.getMessage();
        if (m == null || m.isBlank()) return Textos.t("error.desconocido", t.getClass().getSimpleName());
        if (t instanceof java.nio.file.NoSuchFileException) return Textos.t("error.archivoNoExiste", m);
        if (t instanceof java.nio.file.AccessDeniedException) return Textos.t("error.sinPermiso", m);
        if (m.contains("being used by another process") || m.contains("está siendo utilizado")) return Textos.t("error.archivoAbierto");
        return m;
    }

    private static String pila(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
