package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.domain.PerfilMapeo;
import co.edu.ucc.reportes.domain.Plantilla;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

/** Permite a cualquier sección cambiar de sección o abrir el asistente. */
@Component
public class Navegador {

    private PrincipalController principal;

    void registrar(PrincipalController principal) {
        this.principal = principal;
    }

    public void ir(Seccion s) {
        principal.ir(s);
    }

    /** Asistente desde cero (Cmd/Ctrl+N). */
    public void nuevoReporte() {
        principal.<GenerarController>controlador(Seccion.GENERAR).reiniciar(null, null);
        principal.ir(Seccion.GENERAR);
    }

    /** Asistente con plantilla y mapeo del perfil listos. */
    public void usarPerfil(PerfilMapeo perfil) {
        principal.<GenerarController>controlador(Seccion.GENERAR).reiniciar(perfil, null);
        principal.ir(Seccion.GENERAR);
    }

    public void usarPlantilla(Plantilla plantilla) {
        principal.<GenerarController>controlador(Seccion.GENERAR).reiniciar(null, plantilla);
        principal.ir(Seccion.GENERAR);
    }

    /** Cmd/Ctrl+O: abre la sección Datos y el diálogo de archivo. */
    public void importarDatos() {
        principal.ir(Seccion.DATOS);
        principal.<DatosController>controlador(Seccion.DATOS).importar();
    }

    public Window ventana() {
        return principal == null ? null : principal.ventana();
    }
}
