package co.edu.ucc.reportes.ui;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Abre carpetas y archivos con la aplicación del sistema (Explorador, Finder, Word…). */
@Component
public class Escritorio {

    private final Vistas vistas;

    public Escritorio(Vistas vistas) {
        this.vistas = vistas;
    }

    public void abrir(Path ruta) {
        File archivo = ruta.toFile();
        // En otro hilo: en algunos Linux Desktop.open tarda o bloquea
        Thread hilo = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(archivo);
                    return;
                }
            } catch (Exception ignorada) {
                // se intenta de otra forma
            }
            try {
                String so = System.getProperty("os.name").toLowerCase(Locale.ROOT);
                if (so.contains("linux")) {
                    new ProcessBuilder("xdg-open", archivo.getAbsolutePath()).start();
                } else if (vistas.hostServices() != null) {
                    vistas.hostServices().showDocument(archivo.toURI().toString());
                }
            } catch (Exception ignorada) {
                // sin opciones: la ruta se muestra en pantalla
            }
        }, "abrir-archivo");
        hilo.setDaemon(true);
        hilo.start();
    }
}
