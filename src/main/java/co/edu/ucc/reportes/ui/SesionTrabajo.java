package co.edu.ucc.reportes.ui;

import co.edu.ucc.reportes.service.LectorDatos;
import co.edu.ucc.reportes.service.Problema;
import co.edu.ucc.reportes.service.TablaDatos;
import co.edu.ucc.reportes.service.ValidadorDatos;
import java.nio.file.Path;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import org.springframework.stereotype.Component;

/**
 * Los datos con los que se está trabajando: los comparten la sección Datos y el paso 1
 * del asistente, así no hay que importar el archivo dos veces.
 */
@Component
public class SesionTrabajo {

    /** Resultado de leer: la tabla y sus problemas de validación. */
    public record DatosCargados(TablaDatos tabla, List<String> hojas, List<Problema> problemas) {

        public boolean hayErrores() {
            return ValidadorDatos.hayErrores(problemas);
        }
    }

    private final LectorDatos lector;
    private final ObjectProperty<DatosCargados> datos = new SimpleObjectProperty<>();

    public SesionTrabajo(LectorDatos lector) {
        this.lector = lector;
    }

    public ObjectProperty<DatosCargados> datosProperty() {
        return datos;
    }

    public DatosCargados getDatos() {
        return datos.get();
    }

    /** Lee en segundo plano. Quien la usa pone el resultado con datosProperty().set(...). */
    public Task<DatosCargados> tareaLeer(Path archivo, String hoja) {
        Task<DatosCargados> tarea = new Task<>() {
            @Override
            protected DatosCargados call() throws Exception {
                List<String> hojas = lector.hojas(archivo);
                String h = hoja != null && hojas.contains(hoja) ? hoja : (hojas.isEmpty() ? null : hojas.get(0));
                TablaDatos t = lector.leer(archivo, h);
                return new DatosCargados(t, hojas, ValidadorDatos.validar(t));
            }
        };
        return tarea;
    }

    public static void ejecutar(Task<?> tarea, String nombre) {
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }
}
