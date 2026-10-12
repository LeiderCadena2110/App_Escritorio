package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.service.ServicioAjustes;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import org.springframework.stereotype.Component;

/** Tema claro u oscuro (Configuración › Apariencia). Se aplica al instante. */
@Component
public class Tema {

    private static final String BASE = "/css/base.css";
    private static final String OSCURO = "/css/oscuro.css";

    private final ServicioAjustes ajustes;
    private Scene escenaPrincipal;

    public Tema(ServicioAjustes ajustes) {
        this.ajustes = ajustes;
    }

    public void aplicar(Scene escena) {
        this.escenaPrincipal = escena;
        recargar(escena);
    }

    public void cambiar(String tema) {
        ajustes.guardar(ServicioAjustes.TEMA, tema);
        if (escenaPrincipal != null) recargar(escenaPrincipal);
    }

    /** Los diálogos usan el mismo tema que la ventana. */
    public void aplicar(DialogPane panel) {
        panel.getStylesheets().setAll(hojas());
    }

    private void recargar(Scene escena) {
        escena.getStylesheets().setAll(hojas());
    }

    private String[] hojas() {
        String base = Tema.class.getResource(BASE).toExternalForm();
        if (ServicioAjustes.TEMA_OSCURO.equals(ajustes.tema())) {
            return new String[]{base, Tema.class.getResource(OSCURO).toExternalForm()};
        }
        return new String[]{base};
    }
}
